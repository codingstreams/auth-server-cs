package com.example.authserver.service.appuser;

import com.example.authserver.controller.dto.UpdateUserProfileRequest;
import com.example.authserver.controller.dto.UserProfileUpdateResponse;
import com.example.authserver.mapper.UserMapper;
import com.example.authserver.model.AppUser;
import com.example.authserver.repo.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService {
  private final AppUserRepository appUserRepository;
  private final UserMapper userMapper;

  @Override
  @Transactional
  public UserProfileUpdateResponse updateUserProfile(UpdateUserProfileRequest updateUserProfileRequest) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
      log.warn("Unauthorized attempt to update profile without valid authentication");
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
    }

    final String username = authentication.getName();
    log.info("Updating user profile for username: {}", username);

    AppUser appUser = appUserRepository.findByUsername(username)
        .orElseThrow(() -> {
          log.warn("User with username '{}' not found for profile update", username);
          return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with username: " + username);
        });

    appUser.setFullName(updateUserProfileRequest.fullName());
    AppUser savedUser = appUserRepository.save(appUser);
    log.info("Successfully updated user profile for user: {}, new full name: {}", username, savedUser.getFullName());

    return userMapper.toUserProfileUpdateResponse(savedUser);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AppUser> findByEmail(String email) {
    log.debug("Finding user by email: {}", email);
    return appUserRepository.findByUsername(email);
  }
}
