package ru.college.carmarketplace.service.impl;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.college.carmarketplace.service.LogoutService;
import ru.college.carmarketplace.repo.TokenRepository;

@Service
@RequiredArgsConstructor
public class LogoutServiceImpl implements LogoutHandler, LogoutService {

  private final TokenRepository tokenRepository;
  private final JwtServiceImpl jwtServiceImpl;
  private final int TOKEN_INDEX = 7;

  @Override
  public void logout(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) {
    final String authHeader = request.getHeader("Authorization");
    if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
      return;
    }
    String refreshToken = extractRefreshTokenFromCookie(request);

    if (refreshToken == null) {
      return;
    }
    var storedToken = tokenRepository.findByToken(refreshToken)
        .orElse(null);

    if (storedToken != null) {
      storedToken.setExpired(true);
      storedToken.setRevoked(true);
      tokenRepository.save(storedToken);
      SecurityContextHolder.clearContext();
    }
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

  public String isAccessExpired(String header) {
    if(header != null && header.startsWith("Bearer ")) {
      String token = header.substring(TOKEN_INDEX);
      if(jwtServiceImpl.isTokenExpired(token)){
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
      }
      return "Токен действителен";
    }
    return "Токена нет";

  }
}
