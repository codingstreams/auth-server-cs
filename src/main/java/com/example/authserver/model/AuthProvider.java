package com.example.authserver.model;

public enum AuthProvider {
  LOCAL,
  GOOGLE,
  GITHUB;

  public static AuthProvider fromString(String provider) {
    if (provider == null || provider.isBlank()) {
      return null;
    }
    for (AuthProvider p : values()) {
      if (p.name().equalsIgnoreCase(provider.trim())) {
        return p;
      }
    }
    throw new IllegalArgumentException("Unknown auth provider: " + provider);
  }
}
