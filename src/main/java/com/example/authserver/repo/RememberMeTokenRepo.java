package com.example.authserver.repo;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RememberMeTokenRepo implements PersistentTokenRepository {

  private final RedisTemplate<String, Object> redisTemplate;

  // Adjust TTL to match your remember-me validity duration (e.g., 30 days)
  private static final long TOKEN_VALIDITY_DAYS = 30;

  private String getSeriesKey(String series) {
    return "remember_me:series:" + series;
  }

  private String getUserKey(String username) {
    return "remember_me:user:" + username;
  }

  @Override
  public void createNewToken(@NonNull PersistentRememberMeToken token) {
    String seriesKey = getSeriesKey(token.getSeries());
    String userKey = getUserKey(token.getUsername());

    Map<String, Object> tokenData = new HashMap<>();
    tokenData.put("username", token.getUsername());
    tokenData.put("series", token.getSeries());
    tokenData.put("tokenValue", token.getTokenValue());
    tokenData.put("date", token.getDate().getTime());

    // Save token details by series key
    redisTemplate.opsForHash().putAll(seriesKey, tokenData);
    redisTemplate.expire(seriesKey, Expiration.from(TOKEN_VALIDITY_DAYS, TimeUnit.DAYS));

    // Map series ID under the user index
    redisTemplate.opsForSet().add(userKey, token.getSeries());
    redisTemplate.expire(userKey, Expiration.from(TOKEN_VALIDITY_DAYS, TimeUnit.DAYS));
  }

  @Override
  public void updateToken(@NonNull String series, @NonNull String tokenValue, @NonNull Date lastUsed) {
    String seriesKey = getSeriesKey(series);

    if (Boolean.TRUE.equals(redisTemplate.hasKey(seriesKey))) {
      redisTemplate.opsForHash().put(seriesKey, "tokenValue", tokenValue);
      redisTemplate.opsForHash().put(seriesKey, "date", lastUsed.getTime());

      // Refresh TTL on token update
      redisTemplate.expire(seriesKey, Expiration.from(TOKEN_VALIDITY_DAYS, TimeUnit.DAYS));
    }
  }

  @Override
  public @Nullable PersistentRememberMeToken getTokenForSeries(@NonNull String seriesId) {
    String seriesKey = getSeriesKey(seriesId);
    Map<Object, Object> tokenData = redisTemplate.opsForHash().entries(seriesKey);

    if (tokenData.isEmpty()) {
      return null;
    }

    String username = (String) tokenData.get("username");
    String series = (String) tokenData.get("series");
    String tokenValue = (String) tokenData.get("tokenValue");
    Long time = (Long) tokenData.get("date");

    if (username == null || series == null || tokenValue == null || time == null) {
      return null;
    }

    return new PersistentRememberMeToken(username, series, tokenValue, new Date(time));
  }

  @Override
  public void removeUserTokens(@NonNull String username) {
    String userKey = getUserKey(username);

    // Fetch all active series associated with the user
    Set<Object> seriesSet = redisTemplate.opsForSet().members(userKey);

    if (seriesSet != null && !seriesSet.isEmpty()) {
      for (Object series : seriesSet) {
        if (series instanceof String seriesId) {
          redisTemplate.delete(getSeriesKey(seriesId));
        }
      }
    }

    // Delete the user set index itself
    redisTemplate.delete(userKey);
  }
}
