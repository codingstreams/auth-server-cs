package com.example.authserver.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record AvatarResponse(
    @NotBlank(message = "Seed is required")
    String seed,

    @NotBlank(message = "URL is required")
    String url
) {
}
