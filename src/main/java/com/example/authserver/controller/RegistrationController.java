package com.example.authserver.controller;

import com.example.authserver.controller.dto.AuthSuccessResponse;
import com.example.authserver.controller.dto.RegisterRequest;
import com.example.authserver.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<AuthSuccessResponse> register(@Valid @RequestBody RegisterRequest request) {
    log.info("Received registration request for email: {}", request.email());
    final AuthSuccessResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
