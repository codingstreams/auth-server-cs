package com.example.authserver.controller.dto;

import com.example.authserver.Maskable;
import com.example.authserver.MaskedField;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@MaskedField(value = {"password"})
public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @MaskedField
    String password,

    @NotBlank(message = "Full Name is required")
    @Size(min = 1, message = "Full Name must not be blank")
    String fullName
) implements Maskable {

  @Override
  public String toMaskedString() {
    return "RegisterRequest[email=" + email + ", password=******, fullName=" + fullName + "]";
  }

  @Override
  public String toString() {
    return toMaskedString();
  }
}
