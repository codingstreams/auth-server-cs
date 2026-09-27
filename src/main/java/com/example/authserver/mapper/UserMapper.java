package com.example.authserver.mapper;

import com.example.authserver.controller.dto.AuthSuccessResponse;
import com.example.authserver.controller.dto.SuccessUserRegisterResponse;
import com.example.authserver.controller.dto.UserProfileUpdateResponse;
import com.example.authserver.model.AppUser;
import com.example.authserver.model.Authority;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(source = "username", target = "email")
  @Mapping(target = "displayName", ignore = true)
  @Mapping(target = "roles", source = "authorities", qualifiedByName = "authoritiesToRoles")
  AuthSuccessResponse toAuthSuccessResponse(AppUser appUser);

  @Named("authoritiesToRoles")
  default Set<String> authoritiesToRoles(Set<Authority> authorities) {
    if (authorities == null) {
      return Set.of();
    }
    return authorities.stream()
        .map(Authority::getAuthority)
        .collect(Collectors.toSet());
  }

  @Mapping(source = "username", target = "email")
  @Mapping(target = "message", constant = "User registered successfully")
  SuccessUserRegisterResponse toSuccessUserRegisterResponse(AppUser appUser);

  UserProfileUpdateResponse toUserProfileUpdateResponse(AppUser appUser);
}
