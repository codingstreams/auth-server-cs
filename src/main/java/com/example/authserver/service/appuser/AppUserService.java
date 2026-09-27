package com.example.authserver.service.appuser;

import com.example.authserver.controller.dto.UpdateUserProfileRequest;
import com.example.authserver.controller.dto.UserProfileUpdateResponse;
import com.example.authserver.model.AppUser;

import java.util.Optional;

public interface AppUserService {
  UserProfileUpdateResponse updateUserProfile(UpdateUserProfileRequest updateUserProfileRequest);

  Optional<AppUser> findByEmail(String email);
}

