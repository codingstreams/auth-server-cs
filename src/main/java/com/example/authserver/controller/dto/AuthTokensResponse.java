package com.example.authserver.controller.dto;

import com.example.authserver.Maskable;
import com.example.authserver.MaskedField;

@MaskedField(value = {"accessToken", "refreshToken"})
public record AuthTokensResponse(
    @MaskedField
    String accessToken,

    @MaskedField
    String refreshToken
) implements Maskable {

  @Override
  public String toMaskedString() {
    return "AuthTokensResponse[accessToken=******, refreshToken=******]";
  }

  @Override
  public String toString() {
    return toMaskedString();
  }
}
