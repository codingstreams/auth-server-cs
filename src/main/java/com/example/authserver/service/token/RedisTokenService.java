package com.example.authserver.service.token;

import java.time.Duration;

public interface RedisTokenService {

  String REFRESH_PREFIX = "refresh_token:";
  String BLACKLIST_PREFIX = "blacklist_token:";

  // --- 1. Refresh Token Operations ---
  String createRefreshToken(String userId, Duration ttl);

  String validateAndGetUserId(String refreshToken);

  void deleteRefreshToken(String refreshToken);

  // --- 2. Access Token Blacklisting Operations ---
  void blacklistAccessToken(String tokenId, Duration remainingTtl);

  boolean isBlacklisted(String tokenId);
}
