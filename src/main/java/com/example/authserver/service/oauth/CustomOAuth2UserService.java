package com.example.authserver.service.oauth;

import com.example.authserver.model.AppUser;
import com.example.authserver.model.AuthProvider;
import com.example.authserver.model.Authority;
import com.example.authserver.repo.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final AppUserRepository appUserRepository;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    final OAuth2User oAuth2User = super.loadUser(userRequest);
    final String registrationId = userRequest.getClientRegistration().getRegistrationId();

    log.debug("Processing OAuth2 login for provider: {}", registrationId);

    final OAuth2UserInfo userInfo;
    if ("google".equalsIgnoreCase(registrationId)) {
      userInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
    } else if ("github".equalsIgnoreCase(registrationId)) {
      userInfo = new GithubOAuth2UserInfo(oAuth2User.getAttributes());
    } else {
      log.error("Unsupported OAuth2 provider: {}", registrationId);
      throw new OAuth2AuthenticationException(
          new OAuth2Error("unsupported_provider"),
          "Unsupported OAuth2 provider: " + registrationId
      );
    }

    final Set<GrantedAuthority> userAuthorities = processUserAccount(registrationId, userInfo);

    final Set<GrantedAuthority> authorities = new HashSet<>(oAuth2User.getAuthorities());
    authorities.addAll(userAuthorities);

    String userNameAttributeName = userRequest.getClientRegistration()
        .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
    if (userNameAttributeName == null || userNameAttributeName.isBlank()) {
      userNameAttributeName = "google".equalsIgnoreCase(registrationId) ? "sub" : "id";
    }

    return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), userNameAttributeName);
  }

  @Transactional
  private Set<GrantedAuthority> processUserAccount(String registrationId, OAuth2UserInfo userInfo) {
    final String email = userInfo.getEmail();
    if (Objects.isNull(email) || email.isBlank()) {
      log.warn("OAuth provider '{}' did not provide an email address for userId: {}", registrationId, userInfo.getId());
      throw new OAuth2AuthenticationException(
          new OAuth2Error("missing_email"),
          "Email address not found from OAuth2 provider: " + registrationId
      );
    }

    AuthProvider authProvider;
    try {
      authProvider = AuthProvider.fromString(registrationId);
    } catch (IllegalArgumentException ex) {
      authProvider = AuthProvider.LOCAL;
    }

    final Set<GrantedAuthority> authorities = new HashSet<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

    final Optional<AppUser> userOptional = appUserRepository.findByUsername(email);

    if (userOptional.isPresent()) {
      final AppUser existingUser = userOptional.get();
      log.info("Existing user found for email: {} from provider: {}", email, registrationId);

      // Link OAuth provider to existing user if not already linked
      if (existingUser.getProviderId() == null) {
        existingUser.setProvider(authProvider);
        existingUser.setProviderId(userInfo.getId());
        appUserRepository.save(existingUser);
        log.info("Linked existing user '{}' to provider '{}'", email, registrationId);
      }

      if (existingUser.getAuthorities() != null) {
        existingUser.getAuthorities().forEach(auth ->
            authorities.add(new SimpleGrantedAuthority(auth.getAuthority())));
      }
    } else {
      log.info("Creating new user account for email: {} via OAuth provider: {}", email, registrationId);

      final AppUser newUser = AppUser.builder()
          .username(email)
          .passwordHash(null)
          .provider(authProvider)
          .providerId(userInfo.getId())
          .avatarSeed(com.example.authserver.util.AvatarUtil.generateSeed())
          .active(true)
          .accountNonBlocked(true)
          .build();

      final Authority authority = Authority.builder()
          .user(newUser)
          .authority("ROLE_USER")
          .build();

      newUser.setAuthorities(new HashSet<>(Set.of(authority)));
      appUserRepository.save(newUser);
      log.info("Successfully registered new OAuth user '{}'", email);
    }

    return authorities;
  }
}