package com.example.authserver.security.filter;

import com.example.authserver.service.JwtService;
import com.example.authserver.service.token.RedisTokenService;
import com.example.authserver.util.CookieUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String ACCESS_TOKEN_COOKIE = "access_token";
  private static final String ACCESS_TOKEN_CAMEL_COOKIE = "accessToken";

  private final JwtService jwtService;
  private final RedisTokenService redisTokenService;

  public static String extractBearerToken(HttpServletRequest request) {
    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
      final String token = authHeader.substring(7).trim();
      return token.isEmpty() ? null : token;
    }
    return null;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {

    // 1. Extract token from cookie (or Authorization header as fallback)
    final String token = CookieUtil.extractCookieValue(request, ACCESS_TOKEN_COOKIE)
        .or(() -> CookieUtil.extractCookieValue(request, ACCESS_TOKEN_CAMEL_COOKIE))
        .orElseGet(() -> JwtAuthenticationFilter.extractBearerToken(request));

    if (token != null && jwtService.isTokenValid(token)) {
      try {
        final Claims claims = jwtService.extractClaims(token);
        final String tokenId = claims.getId();

        // 2. Check if the token was logged out early
        final boolean isBlacklisted = (tokenId != null && redisTokenService.isBlacklisted(tokenId))
            || redisTokenService.isBlacklisted(token);

        if (!isBlacklisted) {
          // 3. Register user context for this thread
          if (SecurityContextHolder.getContext().getAuthentication() == null) {
            final String userId = claims.getSubject();
            final List<SimpleGrantedAuthority> authorities = extractAuthorities(claims);

            final UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Successfully registered security context for userId: {}", userId);
          }
        } else {
          log.warn("Access token has been revoked or blacklisted");
          SecurityContextHolder.clearContext();
        }
      } catch (Exception ex) {
        log.error("Failed to authenticate token: {}", ex.getMessage());
        SecurityContextHolder.clearContext();
      }
    }

    filterChain.doFilter(request, response);
  }

  private List<SimpleGrantedAuthority> extractAuthorities(Claims claims) {
    final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    final Object rolesObject = claims.get("roles");

    if (rolesObject instanceof Collection<?> roles) {
      for (Object role : roles) {
        if (role != null) {
          authorities.add(new SimpleGrantedAuthority(role.toString()));
        }
      }
    } else if (rolesObject instanceof String rolesStr && !rolesStr.isBlank()) {
      for (String role : rolesStr.split(",")) {
        final String trimmed = role.trim();
        if (!trimmed.isEmpty()) {
          authorities.add(new SimpleGrantedAuthority(trimmed));
        }
      }
    }

    return authorities;
  }
}
