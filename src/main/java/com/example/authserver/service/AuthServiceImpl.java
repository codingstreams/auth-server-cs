package com.example.authserver.service;

import com.example.authserver.controller.dto.AuthSuccessResponse;
import com.example.authserver.controller.dto.LoginRequest;
import com.example.authserver.controller.dto.RegisterRequest;
import com.example.authserver.exception.UserAlreadyExistsException;
import com.example.authserver.mapper.UserMapper;
import com.example.authserver.model.AppUser;
import com.example.authserver.model.Authority;
import com.example.authserver.repo.AppUserRepository;
import com.example.authserver.service.mail.MailService;
import com.example.authserver.service.token.RedisTokenService;
import com.example.authserver.util.AvatarUtil;
import com.example.authserver.util.CookieUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private static final String ACCESS_TOKEN_COOKIE = "access_token";
  private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
  private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
  private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

  private final AppUserRepository appUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final JwtService jwtService;
  private final RedisTokenService redisTokenService;
  private final MailService mailService;

  @Override
  @Transactional
  public AuthSuccessResponse register(RegisterRequest request) {
    log.info("Processing user registration for email: {}", request.email());

    if (appUserRepository.existsByUsername(request.email())) {
      log.warn("Registration failed. User already exists with email: {}", request.email());
      throw new UserAlreadyExistsException("User with email '" + request.email() + "' already exists");
    }

    final String avatarSeed = AvatarUtil.generateSeed();

    AppUser appUser = AppUser.builder()
        .username(request.email())
        .passwordHash(passwordEncoder.encode(request.password()))
        .avatarSeed(avatarSeed)
        .fullName(request.fullName())
        .active(true)
        .accountNonBlocked(true)
        .build();

    Authority authority = Authority.builder()
        .user(appUser)
        .authority("ROLE_USER")
        .build();

    appUser.setAuthorities(Set.of(authority));

    AppUser savedUser = appUserRepository.save(appUser);
    log.info("Successfully registered user with email: {}", savedUser.getUsername());

    return userMapper.toAuthSuccessResponse(savedUser);
  }

  @Override
  @Transactional(readOnly = true)
  public AuthSuccessResponse login(LoginRequest request) {
    log.info("Processing login authentication for email: {}", request.email());

    final AppUser appUser = appUserRepository.findByUsername(request.email())
        .orElseThrow(() -> {
          log.warn("Login failed: User not found with email: {}", request.email());
          return new BadCredentialsException("Invalid credentials");
        });

    if (!passwordEncoder.matches(request.password(), appUser.getPasswordHash())) {
      log.warn("Login failed: Password mismatch for email: {}", request.email());
      mailService.sendMail(appUser.getUsername(), "Password mismatch");
      throw new BadCredentialsException("Invalid credentials");
    }

    if (!appUser.isActive() || !appUser.isAccountNonBlocked()) {
      log.warn("Login failed: Account disabled or locked for email: {}", request.email());
      mailService.sendMail(appUser.getUsername(), "Account is locked or disabled");
      throw new LockedException("Account is locked or disabled");
    }

    final Set<String> roles = appUser.getAuthorities() != null
        ? appUser.getAuthorities().stream()
        .map(Authority::getAuthority)
        .collect(Collectors.toSet())
        : Set.of();

    final String accessToken = jwtService.generateToken(
        appUser.getUsername(),
        appUser.getUsername(),
        roles,
        ACCESS_TOKEN_TTL.toMinutes()
    );

    final String refreshToken = redisTokenService.createRefreshToken(
        appUser.getUsername(),
        REFRESH_TOKEN_TTL
    );

    final ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs != null && attrs.getResponse() != null) {
      final boolean secure = attrs.getRequest().isSecure();
      CookieUtil.attachCookie(attrs.getResponse(), ACCESS_TOKEN_COOKIE, accessToken, ACCESS_TOKEN_TTL.toSeconds(), secure);
      CookieUtil.attachCookie(attrs.getResponse(), REFRESH_TOKEN_COOKIE, refreshToken, REFRESH_TOKEN_TTL.toSeconds(), secure);
      log.debug("Attached authentication cookies for user: {}", appUser.getUsername());
    }

    log.info("Successfully authenticated user with email: {}", appUser.getUsername());
    return userMapper.toAuthSuccessResponse(appUser);
  }

  @Override
  @Transactional(readOnly = true)
  public String refreshAccessToken(String refreshToken) {
    if (refreshToken.isBlank()) {
      log.warn("Token refresh failed: Refresh token is missing");
      throw new BadCredentialsException("Missing refresh token");
    }

    log.debug("Validating refresh token via Redis");
    final String userId = redisTokenService.validateAndGetUserId(refreshToken);
    if (userId == null) {
      log.warn("Token refresh failed: Refresh token is invalid or expired");
      throw new BadCredentialsException("Invalid or expired refresh token");
    }

    final AppUser appUser = appUserRepository.findByUsername(userId)
        .orElseThrow(() -> {
          log.warn("Token refresh failed: User '{}' not found", userId);
          return new BadCredentialsException("User not found");
        });

    if (!appUser.isActive() || !appUser.isAccountNonBlocked()) {
      log.warn("Token refresh failed: Account is locked or disabled for user '{}'", userId);
      throw new LockedException("Account is locked or disabled");
    }

    final Set<String> roles = appUser.getAuthorities() != null
        ? appUser.getAuthorities().stream()
        .map(Authority::getAuthority)
        .collect(Collectors.toSet())
        : Set.of();

    final String newAccessToken = jwtService.generateToken(
        appUser.getUsername(),
        appUser.getUsername(),
        roles,
        ACCESS_TOKEN_TTL.toMinutes()
    );

    log.info("Successfully refreshed access token for user: {}", appUser.getUsername());
    return newAccessToken;
  }

  @Override
  public void logout(String refreshToken, String accessToken) {
    log.info("Processing logout operation");

    if (!refreshToken.isBlank()) {
      redisTokenService.deleteRefreshToken(refreshToken);
      log.debug("Deleted refresh token from Redis during logout");
    }

    if (!accessToken.isBlank() && jwtService.isTokenValid(accessToken)) {
      try {
        final Claims claims = jwtService.extractClaims(accessToken);
        final Date expiration = claims.getExpiration();
        if (expiration != null) {
          final Duration remainingTtl = Duration.between(Instant.now(), expiration.toInstant());
          if (!remainingTtl.isNegative() && !remainingTtl.isZero()) {
            final String tokenId = claims.getId() != null ? claims.getId() : accessToken;
            redisTokenService.blacklistAccessToken(tokenId, remainingTtl);
            log.debug("Blacklisted access token for duration: {}", remainingTtl);
          }
        }
      } catch (Exception ex) {
        log.warn("Could not extract claims to blacklist token during logout: {}", ex.getMessage());
      }
    }
  }
}
