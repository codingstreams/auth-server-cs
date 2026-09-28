package com.example.authserver.service.oauth;

import com.example.authserver.model.Authority;
import com.example.authserver.repo.AppUserRepository;
import com.example.authserver.service.JwtService;
import com.example.authserver.service.token.RedisTokenService;
import com.example.authserver.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private static final String ACCESS_TOKEN_COOKIE = "access_token";
  private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
  private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
  private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

  private final JwtService jwtService;
  private final RedisTokenService redisTokenService;
  private final AppUserRepository appUserRepository;

  @Value("${security.oauth2.redirect-uri:/dashboard}")
  private String targetUrl = "/dashboard";

  private static @Nullable String getEmail(Authentication authentication) {
    if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
      return null;
    }

    String email = oAuth2User.getAttribute("email");

    if (email == null || email.isBlank()) {
      if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
        final String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        if ("github".equalsIgnoreCase(registrationId)) {
          final OAuth2UserInfo userInfo = new GithubOAuth2UserInfo(oAuth2User.getAttributes());
          email = userInfo.getEmail();
        } else if ("google".equalsIgnoreCase(registrationId)) {
          final OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
          email = userInfo.getEmail();
        }
      }
    }

    if (email == null || email.isBlank()) {
      return null;
    }

    return email;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {

    final String email = getEmail(authentication);
    if (email == null) {
      log.error("OAuth authentication failed: email is null or missing from provider");
      if (!response.isCommitted()) {
        getRedirectStrategy().sendRedirect(request, response, "/login?error=missing_email");
      }
      return;
    }

    final Set<String> roles = appUserRepository.findByUsername(email)
        .map(user -> user.getAuthorities().stream()
            .map(Authority::getAuthority)
            .collect(Collectors.toSet()))
        .filter(set -> !set.isEmpty())
        .orElseGet(() -> {
          final Set<String> auths = authentication.getAuthorities().stream()
              .map(GrantedAuthority::getAuthority)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
          auths.add("ROLE_USER");
          return auths;
        });

    // 1. Generate internal RSA Access Token
    log.debug("Generating internal RSA Access Token for OAuth user: {}", email);
    final String accessToken = jwtService.generateToken(
        email,
        email,
        roles,
        ACCESS_TOKEN_TTL.toMinutes()
    );

    // 2. Generate Refresh Token in Redis
    log.debug("Generating Refresh Token in Redis for OAuth user: {}", email);
    final String refreshToken = redisTokenService.createRefreshToken(
        email,
        REFRESH_TOKEN_TTL
    );

    // 3. Attach cookies
    final boolean secure = request.isSecure();
    CookieUtil.attachCookie(response, ACCESS_TOKEN_COOKIE, accessToken, ACCESS_TOKEN_TTL.toSeconds(), secure);
    CookieUtil.attachCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, REFRESH_TOKEN_TTL.toSeconds(), secure);
    log.info("Attached authentication cookies for OAuth user: {}", email);

    // 4. Redirect to client dashboard
    if (response.isCommitted()) {
      log.debug("Response already committed. Unable to redirect to {}", targetUrl);
      return;
    }

    clearAuthenticationAttributes(request);
    log.info("Redirecting authenticated OAuth user {} to: {}", email, targetUrl);
    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}