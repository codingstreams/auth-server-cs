package com.example.authserver.controller;

import com.example.authserver.controller.dto.AvatarResponse;
import com.example.authserver.model.AppUser;
import com.example.authserver.repo.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AvatarControllerTest {

  private final Map<String, AppUser> mockDb = new HashMap<>();
  private AvatarController avatarController;

  @BeforeEach
  void setUp() {
    mockDb.clear();

    final AppUserRepository stubRepo = (AppUserRepository) Proxy.newProxyInstance(
        AppUserRepository.class.getClassLoader(),
        new Class<?>[]{AppUserRepository.class},
        (proxy, method, args) -> {
          if ("findByUsername".equals(method.getName())) {
            final String username = (String) args[0];
            return Optional.ofNullable(mockDb.get(username));
          }
          return null;
        }
    );

    avatarController = new AvatarController(stubRepo);
  }

  @Test
  @DisplayName("getDefaultAvatar without seed generates random seed and DiceBear 10.x URL")
  void testGetDefaultAvatarRandomSeed() {
    ResponseEntity<AvatarResponse> response = avatarController.getDefaultAvatar(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().seed()).isNotBlank();
    assertThat(response.getBody().url())
        .startsWith("https://api.dicebear.com/10.x/adventurer-neutral/svg?seed=")
        .endsWith(response.getBody().seed());
  }

  @Test
  @DisplayName("getDefaultAvatar with specific seed returns DiceBear 10.x URL using that seed")
  void testGetDefaultAvatarSpecifiedSeed() {
    String seed = "custom-seed-12345";
    ResponseEntity<AvatarResponse> response = avatarController.getDefaultAvatar(seed);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().seed()).isEqualTo(seed);
    assertThat(response.getBody().url())
        .isEqualTo("https://api.dicebear.com/10.x/adventurer-neutral/svg?seed=" + seed);
  }

  @Test
  @DisplayName("getUserAvatar returns saved seed from database for consistent avatar")
  void testGetUserAvatarFound() {
    String username = "alice@example.com";
    String savedSeed = "persisted-avatar-seed-987";
    AppUser mockUser = AppUser.builder()
        .username(username)
        .avatarSeed(savedSeed)
        .build();

    mockDb.put(username, mockUser);

    ResponseEntity<AvatarResponse> response = avatarController.getUserAvatar(username);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().seed()).isEqualTo(savedSeed);
    assertThat(response.getBody().url())
        .isEqualTo("https://api.dicebear.com/10.x/adventurer-neutral/svg?seed=" + savedSeed);
  }

  @Test
  @DisplayName("getUserAvatar returns 404 when user is not found")
  void testGetUserAvatarNotFound() {
    String username = "unknown@example.com";

    ResponseEntity<AvatarResponse> response = avatarController.getUserAvatar(username);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("redirectToAvatar redirects to DiceBear URL")
  void testRedirectToAvatar() {
    String seed = "redirect-seed";
    ResponseEntity<Void> response = avatarController.redirectToAvatar(seed);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
    assertThat(response.getHeaders().getLocation())
        .isEqualTo(URI.create("https://api.dicebear.com/10.x/adventurer-neutral/svg?seed=" + seed));
  }
}
