package com.example.authserver.service;

import com.example.authserver.controller.dto.LoginRequest;
import com.example.authserver.mapper.UserMapper;
import com.example.authserver.model.AppUser;
import com.example.authserver.repo.AppUserRepository;
import com.example.authserver.service.mail.MailService;
import com.example.authserver.service.token.RedisTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

  private AppUserRepository appUserRepository;
  private PasswordEncoder passwordEncoder;
  private UserMapper userMapper;
  private JwtService jwtService;
  private RedisTokenService redisTokenService;
  private MailService mailService;
  private AuthServiceImpl authService;

  @BeforeEach
  void setUp() {
    appUserRepository = Mockito.mock(AppUserRepository.class);
    passwordEncoder = Mockito.mock(PasswordEncoder.class);
    userMapper = Mockito.mock(UserMapper.class);
    jwtService = Mockito.mock(JwtService.class);
    redisTokenService = Mockito.mock(RedisTokenService.class);
    mailService = Mockito.mock(MailService.class);

    authService = new AuthServiceImpl(
        appUserRepository,
        passwordEncoder,
        userMapper,
        jwtService,
        redisTokenService,
        mailService
    );
  }

  @Test
  @DisplayName("login sends email alert when password does not match")
  void testLoginPasswordMismatchSendsEmail() {
    final String email = "user@example.com";
    final AppUser user = AppUser.builder()
        .username(email)
        .passwordHash("encodedPassword")
        .active(true)
        .accountNonBlocked(true)
        .build();

    when(appUserRepository.findByUsername(email)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

    assertThatThrownBy(() -> authService.login(new LoginRequest(email, "wrongPassword")))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Invalid credentials");

    verify(mailService).sendMail(email, "Password mismatch");
  }

  @Test
  @DisplayName("login sends email alert when account is locked or disabled")
  void testLoginLockedAccountSendsEmail() {
    final String email = "locked@example.com";
    final AppUser user = AppUser.builder()
        .username(email)
        .passwordHash("encodedPassword")
        .active(false)
        .accountNonBlocked(true)
        .build();

    when(appUserRepository.findByUsername(email)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);

    assertThatThrownBy(() -> authService.login(new LoginRequest(email, "correctPassword")))
        .isInstanceOf(LockedException.class)
        .hasMessage("Account is locked or disabled");

    verify(mailService).sendMail(email, "Account is locked or disabled");
  }

  @Test
  @DisplayName("login does not send email when user is not found")
  void testLoginUserNotFoundDoesNotSendEmail() {
    final String email = "unknown@example.com";
    when(appUserRepository.findByUsername(email)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(new LoginRequest(email, "password")))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Invalid credentials");

    verify(mailService, never()).sendMail(anyString());
    verify(mailService, never()).sendMail(anyString(), anyString());
  }

  @Test
  @DisplayName("login does not send email alert when credentials are valid")
  void testLoginSuccessDoesNotSendEmail() {
    final String email = "user@example.com";
    final AppUser user = AppUser.builder()
        .username(email)
        .passwordHash("encodedPassword")
        .active(true)
        .accountNonBlocked(true)
        .build();

    when(appUserRepository.findByUsername(email)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
    when(jwtService.generateToken(any(), any(), any(), anyLong())).thenReturn("mockToken");
    when(redisTokenService.createRefreshToken(any(), any(Duration.class))).thenReturn("mockRefreshToken");

    authService.login(new LoginRequest(email, "password123"));

    verify(mailService, never()).sendMail(anyString());
    verify(mailService, never()).sendMail(anyString(), anyString());
  }
}
