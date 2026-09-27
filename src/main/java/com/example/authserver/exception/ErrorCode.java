package com.example.authserver.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", "User already exists"),
  INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "An unexpected internal server error occurred"),
  INVALID_REQUEST("INVALID_REQUEST", "Invalid request parameters");

  private final String errorCode;
  private final String message;
}
