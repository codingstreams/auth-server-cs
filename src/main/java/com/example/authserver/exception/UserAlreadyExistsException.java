package com.example.authserver.exception;

import lombok.Getter;

@Getter
public class UserAlreadyExistsException extends RuntimeException {

  private final ErrorCode errorCode;

  public UserAlreadyExistsException(String message) {
    super(message);
    this.errorCode = ErrorCode.USER_ALREADY_EXISTS;
  }

  public UserAlreadyExistsException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }
}
