package com.example.authserver.service.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenServiceImpl implements RedisTokenService {

  public static final String REFRESH_PREFIX = "refresh_token:";
  public static final String BLACKLIST_PREFIX = "blacklist_token:";
  private static final String BLACKLIST_MARKER = "blacklisted";

  private final StringRedisTemplate stringRedisTemplate;

  @Override
  public String createRefreshToken(String userId, Duration ttl) {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("User ID must not be null or blank");
    }
    if (ttl == null || ttl.isNegative() || ttl.isZero()) {
      throw new IllegalArgumentException("TTL must be a positive duration");
    }

    final String refreshToken = UUID.randomUUID().toString();
    final String key = REFRESH_PREFIX + refreshToken;

    log.info("Creating refresh token for userId: {} with ttl: {}", userId, ttl);
    stringRedisTemplate.opsForValue().set(key, userId, ttl);

    return refreshToken;
  }

  @Override
  public String validateAndGetUserId(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      log.debug("Refresh token validation skipped: token is null or blank");
      return null;
    }

    log.debug("Validating refresh token");
    final String key = REFRESH_PREFIX + refreshToken;
    final String userId = stringRedisTemplate.opsForValue().get(key);

    if (userId != null) {
      log.debug("Refresh token valid for userId: {}", userId);
    } else {
      log.debug("Refresh token not found or expired");
    }

    return userId;
  }

  @Override
  public void deleteRefreshToken(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      log.debug("Refresh token deletion skipped: token is null or blank");
      return;
    }

    log.debug("Deleting refresh token from Redis");
    final String key = REFRESH_PREFIX + refreshToken;
    final Boolean deleted = stringRedisTemplate.delete(key);
    log.debug("Refresh token deleted: {}", Boolean.TRUE.equals(deleted));
  }

  @Override
  public void blacklistAccessToken(String tokenId, Duration remainingTtl) {
    if (tokenId == null || tokenId.isBlank()) {
      log.warn("Access token blacklisting skipped: tokenId is null or blank");
      return;
    }

    if (remainingTtl == null || remainingTtl.isNegative() || remainingTtl.isZero()) {
      log.debug("Access token blacklisting skipped for tokenId: {}: remaining TTL is non-positive", tokenId);
      return;
    }

    log.info("Blacklisting access token ID: {} for remaining duration: {}", tokenId, remainingTtl);
    final String key = BLACKLIST_PREFIX + tokenId;
    stringRedisTemplate.opsForValue().set(key, BLACKLIST_MARKER, remainingTtl);
  }

  @Override
  public boolean isBlacklisted(String tokenId) {
    if (tokenId == null || tokenId.isBlank()) {
      return false;
    }

    final String key = BLACKLIST_PREFIX + tokenId;
    final Boolean exists = stringRedisTemplate.hasKey(key);
    final boolean blacklisted = Boolean.TRUE.equals(exists);

    log.debug("Checked blacklist status for tokenId: {}, blacklisted: {}", tokenId, blacklisted);
    return blacklisted;
  }
}
