package com.example.authserver.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class FaviconControllerTest {

  @Test
  @DisplayName("getFavicon returns 204 No Content")
  void testGetFaviconReturnsNoContent() {
    final FaviconController controller = new FaviconController();
    final ResponseEntity<Void> response = controller.getFavicon();

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
  }
}
