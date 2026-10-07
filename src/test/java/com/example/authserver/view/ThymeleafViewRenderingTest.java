package com.example.authserver.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.AbstractLinkBuilder;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ThymeleafViewRenderingTest {

  private TemplateEngine templateEngine;
  private JakartaServletWebApplication webApp;
  private MockServletContext servletContext;

  @BeforeEach
  void setUp() {
    servletContext = new MockServletContext();
    webApp = JakartaServletWebApplication.buildApplication(servletContext);

    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode("HTML");
    resolver.setCharacterEncoding("UTF-8");

    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setLinkBuilder(new AbstractLinkBuilder() {
      @Override
      public String buildLink(IExpressionContext context, String base, Map<String, Object> parameters) {
        return base;
      }
    });
    this.templateEngine = engine;
  }

  private org.thymeleaf.context.IContext createWebContext(MockHttpServletRequest request) {
    MockHttpServletResponse response = new MockHttpServletResponse();
    IWebExchange webExchange = webApp.buildExchange(request, response);
    return new org.thymeleaf.context.WebContext(webExchange);
  }

  @Test
  @DisplayName("login.html renders successfully with expected fields and submit button")
  void testLoginHtmlRenders() {
    MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
    org.thymeleaf.context.IContext context = createWebContext(request);

    String html = templateEngine.process("login", context);

    assertThat(html).contains("Sign In");
    assertThat(html).contains("id=\"username\"");
    assertThat(html).contains("id=\"password\"");
    assertThat(html).contains("id=\"remember-me\"");
    assertThat(html).contains("id=\"loginBtn\"");
    assertThat(html).contains("class=\"btn btn-primary\"");
  }

  @Test
  @DisplayName("register.html renders successfully with expected fields and submit button")
  void testRegisterHtmlRenders() {
    MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
    org.thymeleaf.context.IContext context = createWebContext(request);

    String html = templateEngine.process("register", context);

    assertThat(html).contains("Create Account");
    assertThat(html).contains("id=\"regName\"");
    assertThat(html).contains("id=\"regEmail\"");
    assertThat(html).contains("id=\"regPassword\"");
    assertThat(html).contains("id=\"regConfirmPassword\"");
    assertThat(html).contains("id=\"registerBtn\"");
  }

  @Test
  @DisplayName("dashboard.html renders with local-password profile")
  void testDashboardLocalPasswordProfile() {
    MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
    org.thymeleaf.context.WebContext context = (org.thymeleaf.context.WebContext) createWebContext(request);
    context.setVariable("displayName", "Alice Smith");
    context.setVariable("username", "alice@example.com");
    context.setVariable("avatarUrl", null);
    context.setVariable("provider", "LOCAL");
    context.setVariable("authorities", List.of(Map.of("authority", "ROLE_USER")));
    context.setVariable("hasPassword", true);
    context.setVariable("googleLinked", false);
    context.setVariable("googleLoginUrl", "/oauth2/authorization/google");
    context.setVariable("githubLinked", false);
    context.setVariable("githubLoginUrl", "/oauth2/authorization/github");
    context.setVariable("canUnlinkOAuth", false);
    context.setVariable("passkeys", Collections.emptyList());

    String html = templateEngine.process("dashboard", context);

    assertThat(html).contains("Alice Smith");
    assertThat(html).contains("Provider: LOCAL");
    assertThat(html).contains("ROLE_USER");
    assertThat(html).contains("Update Password");
    assertThat(html).contains("Save Name");
    assertThat(html).contains("Update Avatar");
    assertThat(html).contains("No passkeys registered yet");
    assertThat(html).contains("Sign Out");
  }

  @Test
  @DisplayName("dashboard.html renders with OAuth profile and populated passkeys")
  void testDashboardOAuthProfileWithPasskeys() {
    MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
    org.thymeleaf.context.WebContext context = (org.thymeleaf.context.WebContext) createWebContext(request);
    context.setVariable("displayName", "Bob OAuth");
    context.setVariable("username", "bob@gmail.com");
    context.setVariable("avatarUrl", "https://example.com/avatar.png");
    context.setVariable("provider", "GOOGLE");
    context.setVariable("authorities", List.of(Map.of("authority", "ROLE_USER"), Map.of("authority", "ROLE_ADMIN")));
    context.setVariable("hasPassword", false);
    context.setVariable("googleLinked", true);
    context.setVariable("githubLinked", false);
    context.setVariable("githubLoginUrl", "/oauth2/authorization/github");
    context.setVariable("canUnlinkOAuth", false);
    context.setVariable("passkeys", List.of(
        Map.of("id", "pk-1", "name", "MacBook Touch ID", "createdAt", "2026-10-01")
    ));

    String html = templateEngine.process("dashboard", context);

    assertThat(html).contains("Bob OAuth");
    assertThat(html).contains("No Local Password Set");
    assertThat(html).contains("Set Password");
    assertThat(html).contains("MacBook Touch ID");
    assertThat(html).contains("Verified");
    assertThat(html).contains("Unlink");
  }

  @Test
  @DisplayName("alerts render error and success messages properly")
  void testAlertsRenderMessages() {
    MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
    request.setParameter("error", "invalid_password");
    request.setParameter("registered", "true");
    org.thymeleaf.context.IContext context = createWebContext(request);

    String html = templateEngine.process("login", context);

    assertThat(html).contains("Current password is incorrect.");
    assertThat(html).contains("Account created successfully! Please sign in.");
  }
}
