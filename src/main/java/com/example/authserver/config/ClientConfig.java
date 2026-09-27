package com.example.authserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.UUID;

@Configuration
public class ClientConfig {

  @Bean
  public RegisteredClientRepository registeredClientRepository(
      PasswordEncoder passwordEncoder,
      @Value("${app.oauth2.client.redirect-uri:http://localhost:3000/oauth2/code/my-web-client}") String redirectUri,
      @Value("${app.oauth2.client.post-logout-redirect-uri:http://localhost:3000}") String postLogoutRedirectUri) {

    final RegisteredClient.Builder clientBuilder = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId("my-web-client")
        // Public client (SPA/Mobile) doesn't use secret, confidential client uses secret:
        .clientSecret(passwordEncoder.encode("my-secret"))
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // Required for public PKCE clients
        // Grants permitted under OAuth 2.1
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        // Standard Scopes
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope("api.read")
        .scope("api.write")
        .clientSettings(ClientSettings.builder()
            .requireAuthorizationConsent(true) // Prompts consent screen
            .requireProofKey(true) // Enforces PKCE
            .build())
        .tokenSettings(TokenSettings.builder()
            .accessTokenTimeToLive(Duration.ofMinutes(15))
            .refreshTokenTimeToLive(Duration.ofDays(7))
            .reuseRefreshTokens(false) // Refresh token rotation
            .build());

    // Whitelisted Callback Redirect URIs
    for (String uri : redirectUri.split(",")) {
      final String trimmedUri = uri.trim();
      if (!trimmedUri.isBlank()) {
        clientBuilder.redirectUri(trimmedUri);
      }
    }

    // Whitelisted Post Logout Redirect URIs
    for (String uri : postLogoutRedirectUri.split(",")) {
      final String trimmedUri = uri.trim();
      if (!trimmedUri.isBlank()) {
        clientBuilder.postLogoutRedirectUri(trimmedUri);
      }
    }

    return new InMemoryRegisteredClientRepository(clientBuilder.build());
  }
}