package com.example.authserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import static org.assertj.core.api.Assertions.assertThat;

class ClientConfigTest {

  @Test
  @DisplayName("registeredClientRepository binds externalized redirect and post-logout URIs")
  void testRegisteredClientRepositoryBindsUris() {
    final ClientConfig clientConfig = new ClientConfig();
    @SuppressWarnings("deprecation")
    final PasswordEncoder passwordEncoder = NoOpPasswordEncoder.getInstance();

    final String redirectUri = "https://app.example.com/callback, http://localhost:3000/oauth2/code/my-web-client";
    final String postLogoutUri = "https://app.example.com/logout, http://localhost:3000";

    final RegisteredClientRepository repository = clientConfig.registeredClientRepository(
        passwordEncoder,
        redirectUri,
        postLogoutUri
    );

    final RegisteredClient client = repository.findByClientId("my-web-client");

    assertThat(client).isNotNull();
    assertThat(client.getRedirectUris()).containsExactlyInAnyOrder(
        "https://app.example.com/callback",
        "http://localhost:3000/oauth2/code/my-web-client"
    );
    assertThat(client.getPostLogoutRedirectUris()).containsExactlyInAnyOrder(
        "https://app.example.com/logout",
        "http://localhost:3000"
    );
  }
}
