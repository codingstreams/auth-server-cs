package com.example.authserver.security.handler;

import com.example.authserver.repo.AppUserRepository;
import com.example.authserver.service.mail.LoginAlertDetails;
import com.example.authserver.service.mail.MailService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
public class FormLoginAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  private final MailService mailService;
  private final AppUserRepository appUserRepository;

  public FormLoginAuthenticationFailureHandler(MailService mailService, AppUserRepository appUserRepository) {
    super("/login?error");
    this.mailService = mailService;
    this.appUserRepository = appUserRepository;
  }

  @Override
  public void onAuthenticationFailure(HttpServletRequest request,
                                      HttpServletResponse response,
                                      AuthenticationException exception) throws IOException, ServletException {
    final String username = request.getParameter("username");
    log.warn("Form login failed for identifier: {}", username);

    if (username != null && !username.isBlank()) {
      final String trimmedEmail = username.trim();
      if (appUserRepository.existsByUsername(trimmedEmail)) {
        final String userAgent = request.getHeader("User-Agent");
        final String failureReason = (exception != null && exception.getMessage() != null)
            ? exception.getMessage()
            : "Invalid credentials";
        final LoginAlertDetails alertDetails = new LoginAlertDetails(
            trimmedEmail,
            userAgent,
            "FAILED",
            failureReason,
            Instant.now()
        );
        mailService.sendMail(alertDetails);
      }
    }

    super.onAuthenticationFailure(request, response, exception);
  }
}
