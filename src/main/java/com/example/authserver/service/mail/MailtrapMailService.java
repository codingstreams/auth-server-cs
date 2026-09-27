package com.example.authserver.service.mail;

import io.mailtrap.client.MailtrapClient;
import io.mailtrap.model.request.emails.Address;
import io.mailtrap.model.request.emails.MailtrapMail;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailtrapMailService implements MailService {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
      .ofPattern("uuuu-MM-dd HH:mm:ss 'UTC'")
      .withZone(ZoneOffset.UTC);

  private final MailtrapClient mailtrapClient;

  @Override
  public void sendMail(String recipient) {
    sendMail(LoginAlertDetails.of(recipient));
  }

  @Override
  public void sendMail(LoginAlertDetails details) {
    if (details == null || details.recipient() == null || details.recipient().isBlank()) {
      log.warn("Recipient email is missing; skipping failed login alert email");
      return;
    }

    final String recipient = details.recipient().trim();
    final String userAgent = resolveUserAgent(details.userAgent());
    final String status = resolveStatus(details.status());
    final String failureReason = resolveFailureReason(details.failureReason());
    final Instant timeOfRequest = resolveTimeOfRequest(details.timeOfRequest());
    final String formattedTime = TIME_FORMATTER.format(timeOfRequest);

    final String emailBody = """
        Hello,

        We detected an unsuccessful login attempt to your account with the following details:

        - Status: %s
        - Failure Reason: %s
        - User Agent: %s
        - Time of Request: %s

        If this was you, you can safely disregard this email. If you did not attempt to sign in, please secure your account by updating your password immediately.

        Best regards,
        Auth Server Security Team
        """.formatted(status, failureReason, userAgent, formattedTime).stripIndent().trim();

    final MailtrapMail mail = MailtrapMail.builder()
        .from(new Address("auth-server@example.com", "Auth Server Security"))
        .to(List.of(new Address(recipient)))
        .subject("Security Alert: Failed Login Attempt")
        .text(emailBody)
        .category("Security Alert")
        .build();

    try {
      log.info("Sending failed login alert email to recipient: {}, userAgent: {}, status: {}, failureReason: {}, time: {}",
          recipient, userAgent, status, failureReason, formattedTime);
      mailtrapClient.send(mail);
      log.info("Successfully sent failed login alert email to: {}", recipient);
    } catch (Exception e) {
      log.error("Failed to send login attempt alert email to recipient {}: {}", recipient, e.getMessage(), e);
    }
  }

  private String resolveUserAgent(String explicitUserAgent) {
    if (explicitUserAgent != null && !explicitUserAgent.isBlank()) {
      return explicitUserAgent.trim();
    }
    try {
      final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
      if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
        final HttpServletRequest request = servletRequestAttributes.getRequest();
        final String header = request.getHeader(HttpHeaders.USER_AGENT);
        if (header != null && !header.isBlank()) {
          return header.trim();
        }
      }
    } catch (Exception e) {
      log.debug("Could not resolve User-Agent from RequestContextHolder: {}", e.getMessage());
    }
    return "Unknown";
  }

  private String resolveStatus(String explicitStatus) {
    if (explicitStatus != null && !explicitStatus.isBlank()) {
      return explicitStatus.trim();
    }
    return "FAILED";
  }

  private String resolveFailureReason(String explicitFailureReason) {
    if (explicitFailureReason != null && !explicitFailureReason.isBlank()) {
      return explicitFailureReason.trim();
    }
    try {
      final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
      if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
        final HttpServletRequest request = servletRequestAttributes.getRequest();
        final Object authException = request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (authException instanceof Throwable throwable && throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
          return throwable.getMessage();
        }
      }
    } catch (Exception e) {
      log.debug("Could not resolve failure reason from RequestContextHolder: {}", e.getMessage());
    }
    return "Invalid credentials";
  }

  private Instant resolveTimeOfRequest(Instant explicitTime) {
    return explicitTime != null ? explicitTime : Instant.now();
  }
}
