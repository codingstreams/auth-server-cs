package com.example.authserver.controller.dto;

import java.util.Set;

public record AuthSuccessResponse(
    String email,
    Set<String> roles,
    String displayName
) {

  public AuthSuccessResponse(String email, Set<String> roles) {
    this(email, roles, null);
  }

  public String username() {
    return email;
  }
}
