package com.example.authserver.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record PasskeyDto(
    @NotBlank String id,
    @NotBlank String name,
    @NotBlank String createdAt
) {
}
