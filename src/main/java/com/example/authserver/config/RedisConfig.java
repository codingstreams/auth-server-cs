package com.example.authserver.config;

import io.lettuce.core.RedisURI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

@Slf4j
@Configuration
public class RedisConfig {

  @Bean
  public RedisConnectionFactory redisConnectionFactory(@Value("${redis.uri}") String uri) {
    log.info("Initializing Redis connection factory");
    final var redisUri = RedisURI.create(uri);
    log.debug("Connecting to Redis host: {}, port: {}, database: {}, ssl: {}",
        redisUri.getHost(), redisUri.getPort(), redisUri.getDatabase(), redisUri.isSsl());

    final var clientConfigBuilder =
        LettuceClientConfiguration.builder();

    if (redisUri.isSsl()) {
      log.debug("Enabling SSL for Lettuce client configuration");
      clientConfigBuilder.useSsl();
    }

    final var clientConfig = clientConfigBuilder.build();
    final var config = LettuceConnectionFactory.createRedisConfiguration(redisUri);

    log.info("RedisConnectionFactory successfully configured");
    return new LettuceConnectionFactory(config, clientConfig);
  }

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    log.info("Initializing RedisCacheManager with 1 hour default TTL and JSON serialization");
    final var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofHours(1)) // Fallback TTL
        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));

    final var cacheManager = RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .build();

    log.info("RedisCacheManager successfully configured");
    return cacheManager;
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    final var template = new RedisTemplate<String, Object>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(RedisSerializer.string());
    template.setValueSerializer(RedisSerializer.json());
    template.afterPropertiesSet();

    return template;
  }
}