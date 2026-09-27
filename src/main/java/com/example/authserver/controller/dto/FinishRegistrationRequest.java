package com.example.authserver.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record FinishRegistrationRequest(
    @NotBlank(message = "Credential ID is required")
    String credentialId,

    @NotBlank(message = "Attestation object is required")
    String attestationObject,

    @NotBlank(message = "Client data JSON is required")
    String clientDataJSON,

    @NotBlank(message = "Label is required")
    String label
) {
}