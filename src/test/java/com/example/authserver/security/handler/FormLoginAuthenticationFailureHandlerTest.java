package com.example.authserver.security.handler;

import com.example.authserver.repo.AppUserRepository;
import com.example.authserver.service.mail.LoginAlertDetails;
import com.example.authserver.service.mail.MailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormLoginAuthenticationFailureHandlerTest {

  private MailService mailService;
  private AppUserRepository appUserRepository;
  private FormLoginAuthenticationFailureHandler failureHandler;

  private HttpServletRequest request;
  private HttpServletResponse response;
  private HttpSession session;

  @BeforeEach
  void setUp() {
    mailService = Mockito.mock(MailService.class);
    appUserRepository = Mockito.mock(AppUserRepository.class);
    failureHandler = new FormLoginAuthenticationFailureHandler(mailService, appUserRepository);

    request = Mockito.mock(HttpServletRequest.class);
    response = Mockito.mock(HttpServletResponse.class);
    session = Mockito.mock(HttpSession.class);

    when(request.getContextPath()).thenReturn("");
    when(request.getSession()).thenReturn(session);
    when(request.getSession(anyBoolean())).thenReturn(session);
    when(response.encodeRedirectURL(anyString())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  @DisplayName("onAuthenticationFailure sends mail alert with details when user is registered")
  void testFailureSendsMailWhenUserExists() throws Exception {
    final String email = "user@example.com";
    when(request.getParameter("username")).thenReturn(email);
    when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 FormLoginBrowser");
    when(appUserRepository.existsByUsername(email)).thenReturn(true);

    failureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("Bad credentials"));

    final ArgumentCaptor<LoginAlertDetails> captor = ArgumentCaptor.forClass(LoginAlertDetails.class);
    verify(mailService).sendMail(captor.capture());

    final LoginAlertDetails capturedDetails = captor.getValue();
    assertThat(capturedDetails.recipient()).isEqualTo(email);
    assertThat(capturedDetails.userAgent()).isEqualTo("Mozilla/5.0 FormLoginBrowser");
    assertThat(capturedDetails.status()).isEqualTo("FAILED");
    assertThat(capturedDetails.failureReason()).isEqualTo("Bad credentials");
    assertThat(capturedDetails.timeOfRequest()).isNotNull();

    verify(response).sendRedirect("/login?error");
  }

  @Test
  @DisplayName("onAuthenticationFailure does not send mail when user is not registered")
  void testFailureDoesNotSendMailWhenUserDoesNotExist() throws Exception {
    final String email = "ghost@example.com";
    when(request.getParameter("username")).thenReturn(email);
    when(appUserRepository.existsByUsername(email)).thenReturn(false);

    failureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("Bad credentials"));

    verify(mailService, never()).sendMail(any(LoginAlertDetails.class));
    verify(mailService, never()).sendMail(anyString());
    verify(response).sendRedirect("/login?error");
  }

  @Test
  @DisplayName("onAuthenticationFailure does not send mail when username is null or blank")
  void testFailureDoesNotSendMailWhenUsernameBlank() throws Exception {
    when(request.getParameter("username")).thenReturn("   ");

    failureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("Bad credentials"));

    verify(mailService, never()).sendMail(any(LoginAlertDetails.class));
    verify(mailService, never()).sendMail(anyString());
    verify(response).sendRedirect("/login?error");
  }
}
