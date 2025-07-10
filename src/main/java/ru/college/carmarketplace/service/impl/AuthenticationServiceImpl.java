package ru.college.carmarketplace.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ru.college.carmarketplace.model.AuthenticationResponse;
import ru.college.carmarketplace.model.entities.VerificationCode;
import ru.college.carmarketplace.repo.VerificationCodeRepository;
import ru.college.carmarketplace.model.AuthenticationRequest;
import ru.college.carmarketplace.model.ErrorResponse;
import ru.college.carmarketplace.model.RegisterRequest;
import ru.college.carmarketplace.exception.CustomException;
import ru.college.carmarketplace.exception.ValidationException;
import ru.college.carmarketplace.service.AuthActionProcess;
import ru.college.carmarketplace.service.AuthenticationService;
import ru.college.carmarketplace.model.entities.Token;
import ru.college.carmarketplace.repo.TokenRepository;
import ru.college.carmarketplace.enums.TokenType;
import ru.college.carmarketplace.enums.Role;
import ru.college.carmarketplace.model.ResetResponse;
import ru.college.carmarketplace.model.entities.AppUser;
import ru.college.carmarketplace.repo.UserRepository;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {
  private final UserRepository repository;
  private final TokenRepository tokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtServiceImpl jwtServiceImpl;
  private final AuthenticationManager authenticationManager;
  private final VerificationCodeRepository verificationCodeRepository;
  private final Map<String, AuthActionProcess> authAction = new HashMap<>();
  private static final Integer MIN_PASS_LENGTH = 8;

  @PostConstruct
  public void init() {
    authAction.put("confirmEmail", new ConfirmEmail(verificationCodeRepository, repository));
    authAction.put("recoveryPassword", new RecoverPassword(repository));
  }

  public boolean verify(RegisterRequest registerRequest) {
    authAction.get(registerRequest.getType()).process(registerRequest);
    return true;
  }

  public void register(RegisterRequest registerRequest) {
    ErrorResponse errorResponse = new ErrorResponse();
    if(registerRequest.getName().isEmpty() && registerRequest.getEmail().isEmpty() && registerRequest.getPassword().isEmpty()){
      errorResponse.addError("name", "Поле имя пустое");
      errorResponse.addError("email", "Поле почты пустое");
      errorResponse.addError("password", "Поле пароля пустое");
    }

    if (repository.existsByEmail(registerRequest.getEmail())) {
      errorResponse.addError("email", "такая почта уже существует");
    }

    if(registerRequest.getPassword().length() < MIN_PASS_LENGTH){
      errorResponse.addError("password", "Пароль должен быть не меньше 8 символов");
    }

    if(!errorResponse.getErrors().isEmpty()){
      throw new ValidationException(errorResponse.getErrors());
    }

    var user = AppUser.builder()
            .name(registerRequest.getName())
            .email(registerRequest.getEmail())
            .password(passwordEncoder.encode(registerRequest.getPassword())).
            confirmed(false) // Пользователь не подтвержден
            .build();
    user.setRole(Role.USER);

    repository.save(user);

    String confirmCode = EmailCodeGenerator.generateRandomConfirm();
    sendEmailConfirmation(registerRequest.getEmail(), confirmCode);

    VerificationCode verificationCode = new VerificationCode(registerRequest.getEmail(), confirmCode);
    verificationCodeRepository.save(verificationCode);
  }

  public AuthenticationResponse authenticate(AuthenticationRequest request) {
    ErrorResponse errorResponse = new ErrorResponse();

    if(request.getEmail() == null || request.getEmail().isEmpty()){
      errorResponse.addError("email", "Поле почты не может быть пустым");
    }

    if(request.getPassword() == null || request.getPassword().isEmpty()){
      errorResponse.addError("password", "Поле пароля не может быть пустым");
    }

    if(!errorResponse.getErrors().isEmpty()) {
      throw new ValidationException(errorResponse.getErrors());
    }

    var user = repository.findByEmail(request.getEmail());

    if(user == null){
      errorResponse.addError("email", "Несуществующая почта или неверный пароль");
      errorResponse.addError("password", "Несуществующая почта или неверный пароль");
      throw new ValidationException(errorResponse.getErrors());
    }

    if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
      errorResponse.addError("email", "Несуществующая почта или неверный пароль");
      errorResponse.addError("password", "Несуществующая почта или неверный пароль");
      throw new ValidationException(errorResponse.getErrors());
    }

    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail(),
            request.getPassword()
        )
    );

    var name = user.getName();
    var email = user.getEmail();
    var role = user.getRole();
    var accessToken = jwtServiceImpl.generateToken(user);
    var refreshToken = jwtServiceImpl.generateRefreshToken(user);
    return getAuthenticationResponse(user, refreshToken, accessToken, name, email, role);
  }

  private AuthenticationResponse getAuthenticationResponse(AppUser user, String refreshToken, String accessToken, String name, String email, Role role) {
    revokeAllUserTokens(user);
    saveRefreshToken(user, refreshToken);
    return AuthenticationResponse.builder()
            .accessToken(accessToken)
            .id(user.getId())
            .refreshToken(refreshToken)
            .phoneNumber(user.getPhoneNumber())
            .name(name)
            .email(email)
            .role(role)
            .build();
  }

  private void saveRefreshToken(AppUser user, String refreshToken) {
    var token = Token.builder()
            .user(user)
            .token(refreshToken)
            .tokenType(TokenType.BEARER)
            .expired(false)
            .revoked(false)
            .build();
    tokenRepository.save(token);
  }

  private void revokeAllUserTokens(AppUser user) {
    var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
    if (validUserTokens.isEmpty())
      return;
    validUserTokens.forEach(token -> {
      token.setExpired(true);
      token.setRevoked(true);
    });
    tokenRepository.saveAll(validUserTokens);
  }

    public AuthenticationResponse refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
      final String refreshToken = extractRefreshTokenFromCookie(request);
      if (refreshToken == null) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token is missing");
        return null;
      }
      final String userEmail = jwtServiceImpl.extractUsername(refreshToken);
      if (userEmail != null) {
        var user = repository.findByEmail(userEmail);
        if (jwtServiceImpl.isTokenValid(refreshToken, user)) {
          var accessToken = jwtServiceImpl.generateToken(user);

          var authResponse = AuthenticationResponse.builder()
                  .accessToken(accessToken)
                  .id(user.getId())
                  .name(user.getName())
                  .email(user.getEmail())
                  .role(user.getRole())
                  .build();

          return authResponse;

        } else {
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid refresh token");
        }
      } else {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid refresh token");
      }
      return AuthenticationResponse.builder().build();
    }

  private String extractRefreshTokenFromCookie(HttpServletRequest request) {
    if (request.getCookies() == null){
      return null;
    }
    for (Cookie cookie : request.getCookies()) {
      if ("refreshToken".equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }


  private void sendEmailConfirmation(String emailToSend, String confirmationCode) {
    Email email = EmailBuilder
            .startingBlank()
            .from("CarEmailConfirmation", "enkcarauto@gmail.com")
            .to(emailToSend)
            .withSubject("Код подтверждения")
            .withPlainText("Здравствуйте, ваш код подтверждения аккаунта " + confirmationCode).buildEmail();
      try (Mailer mailer = MailerBuilder
              .withSMTPServer("smtp.gmail.com", 587, "enkcarauto@gmail.com", "mjvy acqf gwdq qclv")
              .withTransportStrategy(TransportStrategy.SMTP_TLS).buildMailer()) {
          mailer.sendMail(email);
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
  }

  private static class EmailCodeGenerator {
    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";

    private static final String STRING_DATA_FOR_RANDOM = CHAR_LOWER + CHAR_UPPER + NUMBER;
    private static final SecureRandom random = new SecureRandom();

    private static String generateRandomConfirm() {
      StringBuilder sb = new StringBuilder();
      for(int i = 0; i < 6; i++){
        int rndCharAt = random.nextInt(STRING_DATA_FOR_RANDOM.length());
        sb.append(STRING_DATA_FOR_RANDOM.charAt(rndCharAt));
      }
      return sb.toString();
    }
  }


  public ResetResponse reset(AuthenticationRequest request) {
    ErrorResponse errorResponse = new ErrorResponse();
    AppUser user = repository.findByEmail(request.getEmail());
    if (user == null) {
      errorResponse.addError("email", "Такая почта не найдена");
      throw new ValidationException(errorResponse.getErrors());
    }

    verificationCodeRepository.deleteByEmail(request.getEmail());
    String confirmCode = EmailCodeGenerator.generateRandomConfirm();
    VerificationCode verificationCode = new VerificationCode(confirmCode, user.getEmail());
    verificationCodeRepository.save(verificationCode);
    sendEmailConfirmation(request.getEmail(), confirmCode);
    return new ResetResponse("Код отправлен на вашу почту");
  }

  public ResetResponse sendResetToExist(AuthenticationRequest request) {
    AppUser user = repository.findByEmail(request.getEmail());
    verificationCodeRepository.deleteByEmail(request.getEmail());
    String confirmCode = EmailCodeGenerator.generateRandomConfirm();
    VerificationCode verificationCode = new VerificationCode(confirmCode, user.getEmail());
    verificationCodeRepository.save(verificationCode);
    sendEmailConfirmation(request.getEmail(), confirmCode);
    return new ResetResponse("Новый код отправлен вам на почту");
  }

  public ResetResponse setPassword(AuthenticationRequest request) {
    ErrorResponse errorResponse = new ErrorResponse();
    AppUser user = repository.findByEmail(request.getEmail());
    if(user == null) {
      errorResponse.addError("email", "Такой почты не существует");
    }

    if (request.getPassword().length() < 8) {
      errorResponse.addError("password", "Пароль не может быть меньше восьми символов");
    }
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setResetPasswordCode(null);
    repository.save(user);
    return new ResetResponse("Пароль успешно изменен");
  }

}
