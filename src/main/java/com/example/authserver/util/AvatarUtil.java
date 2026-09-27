package com.example.authserver.util;

import java.util.UUID;

public interface AvatarUtil {

  String DICEBEAR_BASE_URL = "https://api.dicebear.com/10.x/adventurer-neutral/svg?seed=";

  static String generateSeed() {
    return UUID.randomUUID().toString();
  }

  static String buildAvatarUrl(String seed) {
    final String resolvedSeed = (seed != null && !seed.isBlank()) ? seed : generateSeed();
    return DICEBEAR_BASE_URL + resolvedSeed;
  }
}
