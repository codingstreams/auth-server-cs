package com.example.authserver.controller;

import com.example.authserver.controller.dto.FinishLoginRequest;
import com.example.authserver.controller.dto.FinishRegistrationRequest;
import com.example.authserver.service.passkey.PasskeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/passkeys")
@RequiredArgsConstructor
public class PasskeyController {

  private final PasskeyService passkeyService;

  @PostMapping("/register/start")
  public ResponseEntity<Map<String, Object>> startRegistration(@RequestParam("email") String email) {
    log.info("Starting passkey registration for email: {}", email);
    final Map<String, Object> options = passkeyService.startRegistration(email);
    log.debug("Generated passkey registration options for user: {}", email);
    return ResponseEntity.ok(options);
  }

  @PostMapping("/register/finish")
  public ResponseEntity<Map<String, String>> finishRegistration(
      @RequestParam("email") String email,
      @Valid @RequestBody FinishRegistrationRequest req) {
    log.info("Finishing passkey registration for email: {}, label: {}", email, req.label());
    passkeyService.finishRegistration(
        email,
        req.credentialId(),
        Base64.getUrlDecoder().decode(req.attestationObject()),
        Base64.getUrlDecoder().decode(req.clientDataJSON()),
        req.label()
    );
    log.info("Passkey registration successfully completed for email: {}", email);
    return ResponseEntity.ok(Map.of("message", "Passkey registered successfully"));
  }

  @PostMapping("/login/start")
  public ResponseEntity<Map<String, Object>> startLogin(
      @RequestParam(value = "email", required = false) String email) {
    log.info("Starting passkey login (identifier: {})", email != null ? email : "username-less");
    final Map<String, Object> options = passkeyService.startLogin(email);
    log.debug("Generated passkey login options");
    return ResponseEntity.ok(options);
  }

  @PostMapping("/login/finish")
  public ResponseEntity<Map<String, String>> finishLogin(
      @RequestParam(value = "email", required = false) String email,
      @Valid @RequestBody FinishLoginRequest req,
      HttpServletRequest request,
      HttpServletResponse response) {
    log.info("Finishing passkey login for credential: {}", req.credentialId());
    passkeyService.finishLogin(email, req, request, response);
    log.info("Passkey login completed successfully");
    return ResponseEntity.ok(Map.of("message", "Login successful"));
  }
}