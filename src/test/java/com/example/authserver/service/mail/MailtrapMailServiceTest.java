package com.example.authserver.service.mail;

import io.mailtrap.client.MailtrapClient;
import io.mailtrap.model.request.emails.MailtrapMail;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailtrapMailServiceTest {

  private MailtrapClient mailtrapClient;
  private MailtrapMailService mailService;

  @BeforeEach
  void setUp() {
    mailtrapClient = Mockito.mock(MailtrapClient.class);
    mailService = new MailtrapMailService(mailtrapClient);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  @DisplayName("sendMail sends alert email containing status, failure reason, user agent, and time")
  void testSendMailSuccessWithDetails() {
    final String recipient = "user@example.com";
    final Instant fixedTime = Instant.parse("2026-09-27T10:15:30Z");
    final LoginAlertDetails details = new LoginAlertDetails(
        recipient,
        "Mozilla/5.0 (X11; Linux x86_64)",
        "FAILED",
        "Invalid password credentials",
        fixedTime
    );

    mailService.sendMail(details);

    final ArgumentCaptor<MailtrapMail> mailCaptor = ArgumentCaptor.forClass(MailtrapMail.class);
    verify(mailtrapClient).send(mailCaptor.capture());

    final MailtrapMail sentMail = mailCaptor.getValue();
    assertThat(sentMail).isNotNull();
    assertThat(sentMail.getSubject()).isEqualTo("Security Alert: Failed Login Attempt");
    assertThat(sentMail.getCategory()).isEqualTo("Security Alert");
    assertThat(sentMail.getTo()).hasSize(1);
    assertThat(sentMail.getTo().getFirst().getEmail()).isEqualTo(recipient);
    assertThat(sentMail.getFrom().getEmail()).isEqualTo("auth-server@example.com");

    final String text = sentMail.getText();
    assertThat(text).contains("Status: FAILED");
    assertThat(text).contains("Failure Reason: Invalid password credentials");
    assertThat(text).contains("User Agent: Mozilla/5.0 (X11; Linux x86_64)");
    assertThat(text).contains("Time of Request: 2026-09-27 10:15:30 UTC");
  }

  @Test
  @DisplayName("sendMail automatically resolves user agent from RequestContextHolder when not provided")
  void testSendMailResolvesFromRequestContextHolder() {
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    when(request.getHeader(HttpHeaders.USER_AGENT)).thenReturn("Mozilla/5.0 RequestContextBrowser");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    mailService.sendMail("user@example.com");

    final ArgumentCaptor<MailtrapMail> mailCaptor = ArgumentCaptor.forClass(MailtrapMail.class);
    verify(mailtrapClient).send(mailCaptor.capture());

    final MailtrapMail sentMail = mailCaptor.getValue();
    assertThat(sentMail.getText()).contains("User Agent: Mozilla/5.0 RequestContextBrowser");
    assertThat(sentMail.getText()).contains("Status: FAILED");
    assertThat(sentMail.getText()).contains("Failure Reason: Invalid credentials");
    assertThat(sentMail.getText()).contains("Time of Request:");
  }

  @Test
  @DisplayName("sendMail logs and handles exception gracefully when mailtrap client fails")
  void testSendMailHandlesException() {
    doThrow(new RuntimeException("Mail delivery failed")).when(mailtrapClient).send(any());

    assertThatCode(() -> mailService.sendMail("user@example.com"))
        .doesNotThrowAnyException();

    verify(mailtrapClient).send(any());
  }

  @Test
  @DisplayName("sendMail skips sending when recipient is null or blank")
  void testSendMailBlankRecipient() {
    mailService.sendMail("   ");
    mailService.sendMail((String) null);
    mailService.sendMail(new LoginAlertDetails(" ", null, null, null, null));

    verify(mailtrapClient, never()).send(any());
  }
}
