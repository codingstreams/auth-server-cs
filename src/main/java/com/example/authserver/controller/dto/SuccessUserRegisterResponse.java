package com.example.authserver.controller.dto;

public record SuccessUserRegisterResponse(
    String email,
    String message
) {
}
