package com.example.authserver.controller;

import com.example.authserver.controller.dto.PasskeyDto;
import com.example.authserver.controller.dto.RegisterRequest;
import com.example.authserver.exception.UserAlreadyExistsException;
import com.example.authserver.model.AppUser;
import com.example.authserver.model.AuthProvider;
import com.example.authserver.repo.AppUserRepository;
import com.example.authserver.repo.UserPasskeyRepository;
import com.example.authserver.service.AuthService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthViewController {

  private final AppUserRepository appUserRepository;
  private final UserPasskeyRepository userPasskeyRepository;
  private final AuthService authService;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.social-login.google-url:/oauth2/authorization/google}")
  private String googleLoginUrl;

  @Value("${app.social-login.github-url:/oauth2/authorization/github}")
  private String githubLoginUrl;

  private final Map<String, UserProfileState> profileStateMap = new ConcurrentHashMap<>();

  @ModelAttribute("googleLoginUrl")
  public String googleLoginUrl() {
    return googleLoginUrl;
  }

  @ModelAttribute("githubLoginUrl")
  public String githubLoginUrl() {
    return githubLoginUrl;
  }

  @GetMapping("/login")
  public String login() {
    log.debug("Rendering login view template");
    return "login";
  }

  @GetMapping("/")
  public String index() {
    log.debug("Rendering index view template");
    return "redirect:/dashboard";
  }

  @GetMapping("/register")
  public String register(Model model) {
    log.debug("Rendering register view template");
    final String defaultSeed = com.example.authserver.util.AvatarUtil.generateSeed();
    final String defaultAvatarUrl = com.example.authserver.util.AvatarUtil.buildAvatarUrl(defaultSeed);
    model.addAttribute("avatarSeed", defaultSeed);
    model.addAttribute("avatarUrl", defaultAvatarUrl);
    return "register";
  }

  @PostMapping("/register")
  public String registerSubmit(@RequestParam("name") String name,
                               @RequestParam("email") String email,
                               @RequestParam("password") String password,
                               @RequestParam("confirmPassword") String confirmPassword) {
    log.info("Processing registration request for email: {}", email);

    if (!password.equals(confirmPassword)) {
      log.warn("Registration rejected: passwords do not match for email: {}", email);
      return "redirect:/register?error=password_mismatch";
    }

    try {
      authService.register(new RegisterRequest(email, password, name));

      final UserProfileState profileState = getOrCreateProfileState(email);
      profileState.setAvatarUrl(com.example.authserver.util.AvatarUtil.buildAvatarUrl(null));
      if (name != null && !name.isBlank()) {
        profileState.setDisplayName(name.trim());
        log.info("Saved display name for registered user: {}", email);
      }

      log.info("Registration successful for email: {}, redirecting to /login?registered=true", email);
      return "redirect:/login?registered=true";
    } catch (UserAlreadyExistsException ex) {
      log.warn("Registration failed: user already exists with email: {}", email);
      return "redirect:/register?error=user_exists";
    } catch (Exception ex) {
      log.error("Registration error for email: {}", email, ex);
      return "redirect:/register?error=true";
    }
  }

  @GetMapping("/dashboard")
  public String dashboard(@AuthenticationPrincipal UserDetails userDetails,
                        Authentication authentication,
                        Model model) {
    final String username = extractUsername(authentication, userDetails);
    log.info("Rendering user profile page for user: {}", username);

    final Optional<AppUser> userOptional = appUserRepository.findByUsername(username);

    final boolean hasPassword;
    final String provider;
    final boolean googleLinked;
    final boolean githubLinked;
    final boolean canUnlinkOAuth;
    final Object authorities;

    if (userOptional.isPresent()) {
      final AppUser user = userOptional.get();
      hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
      provider = user.getProvider() != null ? user.getProvider().name() : "LOCAL";
      googleLinked = user.getProvider() == AuthProvider.GOOGLE;
      githubLinked = user.getProvider() == AuthProvider.GITHUB;
      // Unlink is disabled if OAuth is the only login method (i.e. no local password set)
      canUnlinkOAuth = hasPassword;
      authorities = user.getAuthorities();
    } else {
      hasPassword = false;
      provider = "LOCAL";
      googleLinked = false;
      githubLinked = false;
      canUnlinkOAuth = false;
      authorities = userDetails != null ? userDetails.getAuthorities()
          : (authentication != null ? authentication.getAuthorities() : List.of());
    }

    final UserProfileState profileState = getOrCreateProfileState(username);

    // If user has a saved avatar seed in the database, resolve the exact same avatar URL
    if (userOptional.isPresent() && userOptional.get().getAvatarSeed() != null && !userOptional.get().getAvatarSeed().isBlank()) {
      profileState.setAvatarUrl(com.example.authserver.util.AvatarUtil.buildAvatarUrl(userOptional.get().getAvatarSeed()));
    }

    // If user has a saved full name in the database, use it as display name
    if (userOptional.isPresent() && userOptional.get().getFullName() != null && !userOptional.get().getFullName().isBlank()) {
      profileState.setDisplayName(userOptional.get().getFullName());
    }

    // If OAuth principal has name or avatar details not yet in profile, populate defaults
    if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
      if (profileState.getAvatarUrl() == null) {
        String avatar = oAuth2User.getAttribute("avatar_url"); // GitHub
        if (avatar == null) {
          avatar = oAuth2User.getAttribute("picture"); // Google
        }
        if (avatar != null && !avatar.isBlank()) {
          profileState.setAvatarUrl(avatar);
        }
      }
      String oAuthName = oAuth2User.getAttribute("name");
      if (oAuthName != null && !oAuthName.isBlank() && profileState.getDisplayName().equalsIgnoreCase(username)) {
        profileState.setDisplayName(oAuthName);
      }
    }

    model.addAttribute("username", username);
    model.addAttribute("displayName", profileState.getDisplayName());
    model.addAttribute("avatarUrl", profileState.getAvatarUrl());
    model.addAttribute("hasPassword", hasPassword);
    model.addAttribute("provider", provider);
    model.addAttribute("googleLinked", googleLinked);
    model.addAttribute("githubLinked", githubLinked);
    model.addAttribute("canUnlinkOAuth", canUnlinkOAuth);
    model.addAttribute("authorities", authorities);
    final List<com.example.authserver.model.UserPasskey> dbPasskeys = userPasskeyRepository.findAllByUser_Username(username);
    model.addAttribute("passkeys", dbPasskeys);

    return "dashboard";
  }

  @PostMapping("/profile/avatar")
  public String updateAvatar(@RequestParam("avatarUrl") String avatarUrl,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Authentication authentication) {
    final String username = extractUsername(authentication, userDetails);
    log.info("Updating avatar URL for user: {}", username);

    final UserProfileState profileState = getOrCreateProfileState(username);
    profileState.setAvatarUrl(avatarUrl != null && !avatarUrl.isBlank() ? avatarUrl.trim() : null);

    return "redirect:/dashboard?updated=avatar#tab-general";
  }

  @PostMapping("/profile/change-password")
  public String changePassword(@RequestParam(value = "currentPassword", required = false) String currentPassword,
                               @RequestParam("newPassword") String newPassword,
                               @RequestParam("confirmNewPassword") String confirmNewPassword,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Authentication authentication) {
    final String username = extractUsername(authentication, userDetails);
    log.info("Processing password update for user: {}", username);

    if (!newPassword.equals(confirmNewPassword)) {
      log.warn("Password change rejected: new passwords do not match for user: {}", username);
      return "redirect:/dashboard?error=password_mismatch#tab-security";
    }

    final Optional<AppUser> userOpt = appUserRepository.findByUsername(username);
    if (userOpt.isEmpty()) {
      log.warn("Password change failed: user not found: {}", username);
      return "redirect:/login?error=true";
    }

    final AppUser user = userOpt.get();
    final boolean hasExistingPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();

    if (hasExistingPassword) {
      if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
        log.warn("Password change failed: current password incorrect for user: {}", username);
        return "redirect:/dashboard?error=invalid_password#tab-security";
      }
    }

    user.setPasswordHash(passwordEncoder.encode(newPassword));
    appUserRepository.save(user);
    log.info("Password successfully updated in database for user: {}", username);

    return "redirect:/dashboard?updated=password#tab-security";
  }

  @PostMapping("/profile/oauth/unlink")
  public String unlinkOAuth(@RequestParam("provider") String provider,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Authentication authentication) {
    final String username = extractUsername(authentication, userDetails);
    log.info("Processing OAuth unlink request for provider: {} by user: {}", provider, username);

    final Optional<AppUser> userOpt = appUserRepository.findByUsername(username);
    if (userOpt.isEmpty()) {
      return "redirect:/login?error=true";
    }

    final AppUser user = userOpt.get();
    final boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();

    // Prevent lockout if OAuth is the only login method
    if (!hasPassword) {
      log.warn("OAuth unlink rejected: user {} has no local password", username);
      return "redirect:/dashboard?error=cannot_unlink#tab-oauth";
    }

    user.setProvider(AuthProvider.LOCAL);
    user.setProviderId(null);
    appUserRepository.save(user);
    log.info("Successfully unlinked OAuth provider for user: {}", username);

    return "redirect:/dashboard?updated=oauth#tab-oauth";
  }

  @PostMapping("/profile/passkeys/add")
  public String addPasskey(@RequestParam("passkeyName") String passkeyName,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Authentication authentication) {
    final String username = extractUsername(authentication, userDetails);
    log.info("Adding passkey '{}' for user: {}", passkeyName, username);

    final UserProfileState profileState = getOrCreateProfileState(username);
    final String id = UUID.randomUUID().toString().substring(0, 8);
    final String dateStr = DateTimeFormatter.ofPattern("MMM dd, yyyy").format(LocalDate.now());

    profileState.getPasskeys().add(new PasskeyDto(id, passkeyName.trim(), dateStr));
    return "redirect:/dashboard?updated=passkey_added#tab-passkeys";
  }

  @PostMapping("/profile/passkeys/remove")
  public String removePasskey(@RequestParam("passkeyId") String passkeyId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Authentication authentication) {
    final String username = extractUsername(authentication, userDetails);
    log.info("Removing passkey ID '{}' for user: {}", passkeyId, username);

    try {
      userPasskeyRepository.deleteById(UUID.fromString(passkeyId));
      log.info("Deleted passkey with ID '{}' from DB for user: {}", passkeyId, username);
    } catch (Exception ex) {
      log.warn("Could not delete passkey by UUID, checking credential ID: {}", passkeyId);
      userPasskeyRepository.findByCredentialId(passkeyId).ifPresent(userPasskeyRepository::delete);
    }

    final UserProfileState profileState = getOrCreateProfileState(username);
    profileState.getPasskeys().removeIf(pk -> pk.id().equals(passkeyId));

    return "redirect:/dashboard?updated=passkey_removed#tab-passkeys";
  }

  private String extractUsername(Authentication authentication, UserDetails userDetails) {
    if (userDetails != null && userDetails.getUsername() != null && !userDetails.getUsername().isBlank()) {
      return userDetails.getUsername();
    }
    if (authentication != null) {
      if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        if (email != null && !email.isBlank()) {
          return email;
        }
      }
      return authentication.getName();
    }
    return "user@example.com";
  }

  private UserProfileState getOrCreateProfileState(String username) {
    return profileStateMap.computeIfAbsent(username, k -> {
      final UserProfileState state = new UserProfileState();
      final String prefix = k.contains("@") ? k.substring(0, k.indexOf('@')) : k;
      state.setDisplayName(Character.toUpperCase(prefix.charAt(0)) + (prefix.length() > 1 ? prefix.substring(1) : ""));
      return state;
    });
  }

  @Getter
  @Setter
  private static class UserProfileState {
    private final List<PasskeyDto> passkeys = new CopyOnWriteArrayList<>();
    private String displayName;
    private String avatarUrl;
  }
}
