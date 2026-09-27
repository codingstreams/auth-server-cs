package com.example.authserver.service.mail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record LoginAlertDetails(
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    String recipient,

    String userAgent,
    String status,
    String failureReason,
    Instant timeOfRequest
) {

  public static LoginAlertDetails of(String recipient) {
    return new LoginAlertDetails(recipient, null, "FAILED", null, Instant.now());
  }

  public static LoginAlertDetails of(String recipient, String failureReason) {
    return new LoginAlertDetails(recipient, null, "FAILED", failureReason, Instant.now());
  }
}
