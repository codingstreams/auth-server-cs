package com.example.authserver.config;

import io.mailtrap.client.MailtrapClient;
import io.mailtrap.config.MailtrapConfig;
import io.mailtrap.factory.MailtrapClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailtrapClientConfig {
  @Bean
  MailtrapClient mailtrapClient(@Value("${app.mailtrap.token}") String token) {

    final var config = new MailtrapConfig.Builder()
        .sandbox(true)
        .inboxId(2187508L)
        .token(token)
        .build();

    return MailtrapClientFactory.createMailtrapClient(config);
  }
}
