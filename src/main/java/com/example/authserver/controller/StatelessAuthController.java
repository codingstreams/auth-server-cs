package com.example.authserver.controller;

import com.example.authserver.controller.dto.AuthSuccessResponse;
import com.example.authserver.controller.dto.LoginRequest;
import com.example.authserver.security.filter.JwtAuthenticationFilter;
import com.example.authserver.service.AuthService;
import com.example.authserver.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class StatelessAuthController {

  private static final String ACCESS_TOKEN_COOKIE = "access_token";
  private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
  private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<AuthSuccessResponse> login(@Valid @RequestBody LoginRequest request) {
    log.info("Processing login request for user: {}", request.email());
    final AuthSuccessResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
    log.debug("Processing token refresh request");

    final String refreshToken = CookieUtil.extractCookieValue(request, REFRESH_TOKEN_COOKIE)
        .orElseGet(() -> request.getHeader("X-Refresh-Token"));

    final String newAccessToken = authService.refreshAccessToken(refreshToken);

    final boolean secure = request.isSecure();
    CookieUtil.attachCookie(response, ACCESS_TOKEN_COOKIE, newAccessToken, ACCESS_TOKEN_TTL.toSeconds(), secure);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
    log.info("Processing logout request");

    final String refreshToken = CookieUtil.extractCookieValue(request, REFRESH_TOKEN_COOKIE)
        .orElseGet(() -> request.getHeader("X-Refresh-Token"));
    final String accessToken = CookieUtil.extractCookieValue(request, ACCESS_TOKEN_COOKIE)
        .orElseGet(() -> JwtAuthenticationFilter.extractBearerToken(request));

    if (accessToken == null) {
      log.warn("No access token found.");
      return ResponseEntity.noContent().build();
    }

    authService.logout(refreshToken, accessToken);

    final boolean secure = request.isSecure();
    CookieUtil.deleteCookie(response, ACCESS_TOKEN_COOKIE, secure);
    CookieUtil.deleteCookie(response, REFRESH_TOKEN_COOKIE, secure);

    SecurityContextHolder.clearContext();
    return ResponseEntity.noContent().build();
  }
}