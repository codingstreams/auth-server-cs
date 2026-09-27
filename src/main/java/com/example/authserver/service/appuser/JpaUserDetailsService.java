package com.example.authserver.service.appuser;

import com.example.authserver.model.AppUser;
import com.example.authserver.repo.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

  private final AppUserRepository appUserRepository;

  @Override
  @Transactional(readOnly = true)
  public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
    log.debug("Attempting to load user by username: {}", username);

    AppUser appUser = appUserRepository.findByUsername(username)
        .orElseThrow(() -> {
          log.warn("User not found with username: {}", username);
          return new UsernameNotFoundException("User not found with username: " + username);
        });

    List<GrantedAuthority> authorities = appUser.getAuthorities() != null
        ? appUser.getAuthorities().stream()
        .map(authority -> (GrantedAuthority) new SimpleGrantedAuthority(authority.getAuthority()))
        .toList()
        : List.of();

    log.debug("Successfully loaded user '{}' with {} authorities", username, authorities.size());

    return User.withUsername(appUser.getUsername())
        .password(appUser.getPasswordHash())
        .disabled(!appUser.isActive())
        .accountLocked(!appUser.isAccountNonBlocked())
        .authorities(authorities)
        .build();
  }
}
