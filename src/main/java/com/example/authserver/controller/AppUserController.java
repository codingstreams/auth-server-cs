package com.example.authserver.controller;

import com.example.authserver.controller.dto.UpdateUserProfileRequest;
import com.example.authserver.controller.dto.UserProfileUpdateResponse;
import com.example.authserver.service.appuser.AppUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AppUserController {

  private final AppUserService appUserService;

  @PutMapping
  public ResponseEntity<UserProfileUpdateResponse> updateUserProfile(
      @Valid @RequestBody UpdateUserProfileRequest updateUserProfileRequest) {
    log.info("Received request to update user profile");
    final var response = appUserService.updateUserProfile(updateUserProfileRequest);
    log.info("User profile updated successfully with full name: {}", response.fullName());
    return ResponseEntity.ok(response);
  }
}

