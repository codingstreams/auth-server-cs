package com.example.authserver.service.oauth;

import org.jspecify.annotations.Nullable;

public interface OAuth2UserInfo {
  String getId();

  @Nullable String getEmail();

  String getName();
}