package com.example.authserver.controller;

import com.example.authserver.controller.dto.UpdateUserProfileRequest;
import com.example.authserver.controller.dto.UserProfileUpdateResponse;
import com.example.authserver.mapper.UserMapper;
import com.example.authserver.model.AppUser;
import com.example.authserver.repo.AppUserRepository;
import com.example.authserver.service.appuser.AppUserService;
import com.example.authserver.service.appuser.AppUserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppUserControllerTest {

  private final Map<String, AppUser> mockDb = new HashMap<>();
  private AppUserRepository stubRepo;
  private UserMapper stubMapper;
  private AppUserService appUserService;
  private AppUserController appUserController;

  @BeforeEach
  void setUp() {
    mockDb.clear();

    stubRepo = (AppUserRepository) Proxy.newProxyInstance(
        AppUserRepository.class.getClassLoader(),
        new Class<?>[]{AppUserRepository.class},
        (proxy, method, args) -> {
          if ("findByUsername".equals(method.getName())) {
            final String username = (String) args[0];
            return Optional.ofNullable(mockDb.get(username));
          } else if ("save".equals(method.getName())) {
            final AppUser user = (AppUser) args[0];
            mockDb.put(user.getUsername(), user);
            return user;
          }
          return null;
        }
    );

    stubMapper = (UserMapper) Proxy.newProxyInstance(
        UserMapper.class.getClassLoader(),
        new Class<?>[]{UserMapper.class},
        (proxy, method, args) -> {
          if ("toUserProfileUpdateResponse".equals(method.getName())) {
            final AppUser user = (AppUser) args[0];
            return new UserProfileUpdateResponse(user.getFullName());
          }
          return null;
        }
    );

    appUserService = new AppUserServiceImpl(stubRepo, stubMapper);
    appUserController = new AppUserController(appUserService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("updateUserProfile returns 200 OK with updated name when authenticated")
  void testUpdateUserProfileSuccess() {
    final String username = "alice@example.com";
    final AppUser user = AppUser.builder()
        .username(username)
        .fullName("Alice OldName")
        .build();
    mockDb.put(username, user);

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(username, "n/a", List.of())
    );

    final UpdateUserProfileRequest request = new UpdateUserProfileRequest("Alice NewName");
    final ResponseEntity<UserProfileUpdateResponse> response = appUserController.updateUserProfile(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().fullName()).isEqualTo("Alice NewName");
    assertThat(response.getBody().name()).isEqualTo("Alice NewName");
    assertThat(mockDb.get(username).getFullName()).isEqualTo("Alice NewName");
  }

  @Test
  @DisplayName("updateUserProfile throws 401 UNAUTHORIZED when no authentication present")
  void testUpdateUserProfileUnauthenticated() {
    SecurityContextHolder.clearContext();

    final UpdateUserProfileRequest request = new UpdateUserProfileRequest("New Name");

    assertThatThrownBy(() -> appUserController.updateUserProfile(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("updateUserProfile throws 404 NOT_FOUND when authenticated user does not exist in DB")
  void testUpdateUserProfileUserNotFound() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("ghost@example.com", "n/a", List.of())
    );

    final UpdateUserProfileRequest request = new UpdateUserProfileRequest("New Name");

    assertThatThrownBy(() -> appUserController.updateUserProfile(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
  }
}
