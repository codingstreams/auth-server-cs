package com.example.authserver.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record FinishLoginRequest(
    String challengeId,

    @NotBlank(message = "Credential ID is required")
    String credentialId,

    @NotBlank(message = "Client data JSON is required")
    String clientDataJSON,

    @NotBlank(message = "Authenticator data is required")
    String authenticatorData,

    @NotBlank(message = "Signature is required")
    String signature,

    String userHandle
) {}
