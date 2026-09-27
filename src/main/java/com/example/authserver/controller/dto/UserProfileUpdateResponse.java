package com.example.authserver.controller.dto;

public record UserProfileUpdateResponse(
    String fullName
) {
  public String name() {
    return fullName;
  }
}
