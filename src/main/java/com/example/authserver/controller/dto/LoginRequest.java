package com.example.authserver.controller.dto;

import com.example.authserver.Maskable;
import com.example.authserver.MaskedField;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@MaskedField(value = {"password"})
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @MaskedField
    String password
) implements Maskable {

  @Override
  public String toMaskedString() {
    return "LoginRequest[email=" + email + ", password=******]";
  }

  @Override
  public String toString() {
    return toMaskedString();
  }
}
