package com.example.authserver.service.oauth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public class GithubOAuth2UserInfo implements OAuth2UserInfo {

  private final Map<String, Object> attributes;

  @Override
  public String getId() {
    final Object id = attributes.get("id");
    return id != null ? String.valueOf(id) : null;
  }

  @Override
  public String getEmail() {
    final Object email = attributes.get("email");
    return email != null ? String.valueOf(email) : null;
  }

  @Override
  public String getName() {
    final Object name = attributes.get("name");
    if (name != null && !String.valueOf(name).isBlank()) {
      return String.valueOf(name);
    }
    final Object login = attributes.get("login");
    return login != null ? String.valueOf(login) : null;
  }
}
