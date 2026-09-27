package com.example.authserver.controller;

import com.example.authserver.controller.dto.FinishLoginRequest;
import com.example.authserver.controller.dto.FinishRegistrationRequest;
import com.example.authserver.service.passkey.PasskeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasskeyControllerTest {

  private PasskeyService passkeyService;
  private PasskeyController passkeyController;

  @BeforeEach
  void setUp() {
    passkeyService = Mockito.mock(PasskeyService.class);
    passkeyController = new PasskeyController(passkeyService);
  }

  @Test
  @DisplayName("startRegistration returns 200 OK with options map")
  void testStartRegistration() {
    final String email = "user@example.com";
    final Map<String, Object> mockOptions = Map.of("challenge", "dummy-challenge-base64", "rpId", "localhost");
    when(passkeyService.startRegistration(email)).thenReturn(mockOptions);

    final ResponseEntity<Map<String, Object>> response = passkeyController.startRegistration(email);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).containsEntry("challenge", "dummy-challenge-base64");
    assertThat(response.getBody()).containsEntry("rpId", "localhost");
    verify(passkeyService).startRegistration(email);
  }

  @Test
  @DisplayName("finishRegistration decodes base64 fields, invokes service, and returns 200 OK")
  void testFinishRegistration() {
    final String email = "user@example.com";
    final String credentialId = "cred-12345";
    final String attestationBase64 = Base64.getUrlEncoder().encodeToString("dummy-attestation".getBytes());
    final String clientDataBase64 = Base64.getUrlEncoder().encodeToString("dummy-client-data".getBytes());

    final FinishRegistrationRequest req = new FinishRegistrationRequest(
        credentialId,
        attestationBase64,
        clientDataBase64,
        "MacBook Touch ID"
    );

    doNothing().when(passkeyService).finishRegistration(
        eq(email),
        eq(credentialId),
        any(byte[].class),
        any(byte[].class),
        eq("MacBook Touch ID")
    );

    final ResponseEntity<Map<String, String>> response = passkeyController.finishRegistration(email, req);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("message")).isEqualTo("Passkey registered successfully");
    verify(passkeyService).finishRegistration(
        eq(email),
        eq(credentialId),
        any(byte[].class),
        any(byte[].class),
        eq("MacBook Touch ID")
    );
  }

  @Test
  @DisplayName("startLogin with email returns 200 OK with assertion options")
  void testStartLoginWithEmail() {
    final String email = "user@example.com";
    final Map<String, Object> mockOptions = Map.of("challenge", "login-challenge-base64", "rpId", "localhost");
    when(passkeyService.startLogin(email)).thenReturn(mockOptions);

    final ResponseEntity<Map<String, Object>> response = passkeyController.startLogin(email);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).containsEntry("challenge", "login-challenge-base64");
    assertThat(response.getBody()).containsEntry("rpId", "localhost");
    verify(passkeyService).startLogin(email);
  }

  @Test
  @DisplayName("startLogin username-less (without email) returns 200 OK with assertion options")
  void testStartLoginUsernameLess() {
    final Map<String, Object> mockOptions = Map.of("challengeId", "cid-123", "challenge", "login-challenge-base64", "rpId", "localhost");
    when(passkeyService.startLogin(null)).thenReturn(mockOptions);

    final ResponseEntity<Map<String, Object>> response = passkeyController.startLogin(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).containsEntry("challengeId", "cid-123");
    assertThat(response.getBody()).containsEntry("challenge", "login-challenge-base64");
    verify(passkeyService).startLogin(null);
  }

  @Test
  @DisplayName("finishLogin validates assertion and returns 200 OK without email")
  void testFinishLoginUsernameLess() {
    final FinishLoginRequest req = new FinishLoginRequest(
        "challenge-uuid",
        "cred-12345",
        "clientDataJSON-base64",
        "authenticatorData-base64",
        "signature-base64",
        null
    );

    final HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
    final HttpServletResponse mockResponse = Mockito.mock(HttpServletResponse.class);

    doNothing().when(passkeyService).finishLogin(isNull(), eq(req), any(), any());

    final ResponseEntity<Map<String, String>> response = passkeyController.finishLogin(null, req, mockRequest, mockResponse);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("message")).isEqualTo("Login successful");
    verify(passkeyService).finishLogin(isNull(), eq(req), eq(mockRequest), eq(mockResponse));
  }
}
