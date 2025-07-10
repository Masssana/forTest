package ru.college.carmarketplace.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.college.carmarketplace.model.AuthenticationRequest;
import ru.college.carmarketplace.model.AuthenticationResponse;
import ru.college.carmarketplace.model.RegisterRequest;
import ru.college.carmarketplace.service.impl.AuthenticationServiceImpl;
import ru.college.carmarketplace.service.impl.LogoutServiceImpl;
import ru.college.carmarketplace.model.ResetResponse;

import java.io.IOException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

  private final AuthenticationServiceImpl service;
  private final LogoutServiceImpl logoutServiceImpl;



  private static final Integer THIRTY_DAYS_IN_SECONDS = 5184000;

  @PostMapping("/verify/email")
  public ResponseEntity<String> verifyEmail(@RequestBody @Valid RegisterRequest registerRequest) {
      boolean isVerified = service.verify(registerRequest);
      if (isVerified) {
        return ResponseEntity.ok("Email успешно подтвержден.");
      } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ошибка при проверке кода");
      }

  }

  @PostMapping("/registration")
  public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
      service.register(request);
      return ResponseEntity.ok("Пользователь зарегистрирован. Проверьте email для подтверждения.");

  }

  // в сервис
  @PostMapping("/login")
  public ResponseEntity<AuthenticationResponse> authenticate(
          @RequestBody AuthenticationRequest request, HttpServletResponse httpresponse
  ) {
    AuthenticationResponse response = service.authenticate(request);

    ResponseCookie refreshCookie = ResponseCookie
            .from("refreshToken", response.getRefreshToken())
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .path("/")
            .maxAge(THIRTY_DAYS_IN_SECONDS).build();
    httpresponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

    return ResponseEntity.ok(AuthenticationResponse.builder().accessToken(response.getAccessToken())
                    .id(response.getId())
                    .phoneNumber(response.getPhoneNumber())
            .name(response.getName())
            .email(response.getEmail())
            .role(response.getRole()).build());
  }

  @PostMapping("/refresh")
  public AuthenticationResponse refreshToken(
      HttpServletRequest request,
      HttpServletResponse response
  ) throws IOException {
    return service.refreshToken(request, response);
  }

  // в сервис или утил
  @PostMapping("/logout")
  public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                    .maxAge(0)
            .path("/")
            .secure(true)
            .httpOnly(true)
            .sameSite("None")
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    logoutServiceImpl.logout(request, response, authentication);
  }

  //@PostMapping("/reset")
  @PostMapping("/password/reset")
  public ResponseEntity<ResetResponse> reset(@RequestBody AuthenticationRequest request) {
    return ResponseEntity.ok(service.reset(request));
  }

  //@PostMapping("/set")
  @PostMapping("/password/set")
  public ResponseEntity<ResetResponse> set(@RequestBody AuthenticationRequest request)
  {
    return ResponseEntity.ok(service.setPassword(request));
  }

  @PostMapping("/send/new")
  public ResponseEntity<ResetResponse> sendNewCode(@RequestBody AuthenticationRequest request) {
    return ResponseEntity.ok(service.sendResetToExist(request));
  }

  @GetMapping("is/expired")
  public String isExpired(@RequestHeader("Authorization") String header) {
    return logoutServiceImpl.isAccessExpired(header);
  }

}
