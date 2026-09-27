package com.example.authserver.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    String fullName
) {
  public static UpdateUserProfileRequest updateName(String displayName) {
    return new UpdateUserProfileRequest(displayName);
  }
}
