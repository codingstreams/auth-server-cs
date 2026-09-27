package com.example.authserver.service;

import com.example.authserver.controller.dto.AuthSuccessResponse;
import com.example.authserver.controller.dto.LoginRequest;
import com.example.authserver.controller.dto.RegisterRequest;

public interface AuthService {

  AuthSuccessResponse register(RegisterRequest request);

  AuthSuccessResponse login(LoginRequest request);

  String refreshAccessToken(String refreshToken);

  void logout(String refreshToken, String accessToken);
}
