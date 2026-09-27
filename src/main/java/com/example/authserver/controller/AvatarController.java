package com.example.authserver.controller;

import com.example.authserver.controller.dto.AvatarResponse;
import com.example.authserver.repo.AppUserRepository;
import com.example.authserver.util.AvatarUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping({"/api/avatar", "/avatar"})
@RequiredArgsConstructor
public class AvatarController {

  private final AppUserRepository appUserRepository;

  /**
   * Generates or returns a default avatar with a random or specified seed.
   */
  @GetMapping("/default")
  public ResponseEntity<AvatarResponse> getDefaultAvatar(@RequestParam(value = "seed", required = false) String seed) {
    final String avatarSeed = (seed != null && !seed.isBlank()) ? seed.trim() : AvatarUtil.generateSeed();
    final String avatarUrl = AvatarUtil.buildAvatarUrl(avatarSeed);
    log.debug("Generating default avatar response with seed: {}", avatarSeed);
    return ResponseEntity.ok(new AvatarResponse(avatarSeed, avatarUrl));
  }

  /**
   * Retrieves the saved avatar for a registered user to ensure consistent results every time.
   */
  @GetMapping("/user/{username}")
  public ResponseEntity<AvatarResponse> getUserAvatar(@PathVariable("username") String username) {
    log.debug("Retrieving avatar for user: {}", username);
    return appUserRepository.findByUsername(username)
        .map(user -> {
          final String seed = (user.getAvatarSeed() != null && !user.getAvatarSeed().isBlank())
              ? user.getAvatarSeed()
              : AvatarUtil.generateSeed();
          final String url = AvatarUtil.buildAvatarUrl(seed);
          return ResponseEntity.ok(new AvatarResponse(seed, url));
        })
        .orElseGet(() -> {
          log.warn("User '{}' not found while retrieving avatar", username);
          return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        });
  }

  /**
   * Directly redirects to the DiceBear SVG avatar URL.
   */
  @GetMapping("/redirect")
  public ResponseEntity<Void> redirectToAvatar(@RequestParam(value = "seed", required = false) String seed) {
    final String avatarSeed = (seed != null && !seed.isBlank()) ? seed.trim() : AvatarUtil.generateSeed();
    final String avatarUrl = AvatarUtil.buildAvatarUrl(avatarSeed);
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(avatarUrl)).build();
  }
}
