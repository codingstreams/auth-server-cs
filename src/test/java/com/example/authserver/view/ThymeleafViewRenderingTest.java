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
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

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
    var context = (org.thymeleaf.context.WebContext) createWebContext(request);
    context.setVariables(Map.of("googleLoginUrl", "/oauth2/authorization/google", "githubLoginUrl", "/oauth2/authorization/github"));

    String html = templateEngine.process("login", context);

    assertThat(html).contains("Sign In");
    assertThat(html).contains("id=\"username\"");
    assertThat(html).contains("id=\"password\"");
    assertThat(html).contains("id=\"remember-me\"");
    assertThat(html).contains("id=\"loginBtn\"");
    assertThat(html).contains("class=\"btn btn-primary\"");
    assertThat(html.indexOf("class=\"app-navbar\"")).isGreaterThan(html.indexOf("class=\"auth-card\""));
    assertUniqueIds(html);
    writePreview("login", html);
  }

  @Test
  @DisplayName("register.html renders successfully with expected fields and submit button")
  void testRegisterHtmlRenders() {
    MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
    var context = (org.thymeleaf.context.WebContext) createWebContext(request);
    context.setVariables(Map.of("googleLoginUrl", "/oauth2/authorization/google", "githubLoginUrl", "/oauth2/authorization/github"));

    String html = templateEngine.process("register", context);

    assertThat(html).contains("Create Account");
    assertThat(html).contains("id=\"regName\"");
    assertThat(html).contains("id=\"regEmail\"");
    assertThat(html).contains("id=\"regPassword\"");
    assertThat(html).contains("id=\"regConfirmPassword\"");
    assertThat(html).contains("id=\"registerBtn\"");
    assertThat(html).contains("aria-describedby=\"regPassword-hint\"");
    assertThat(html).contains("Use at least 8 characters.");
    assertThat(html).contains("Sign in with an existing passkey");
    assertUniqueIds(html);
    writePreview("register", html);
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
    assertThat(Pattern.compile("<span\\b[^>]*>Save Name</span>").matcher(html).results()).hasSize(1);
    assertThat(Pattern.compile("<span\\b[^>]*>Update Avatar</span>").matcher(html).results()).hasSize(1);
    assertThat(html).contains("No passkeys registered yet");
    assertThat(Pattern.compile("<span\\b[^>]*>Sign Out</span>").matcher(html).results()).hasSize(1);
    assertThat(html).doesNotContain("Sign Out Everywhere", "> Active<", ">Verified<");
    assertThat(html).contains("id=\"avatarInitials\"", "id=\"avatarPreviewStatus\"");
    assertThat(Pattern.compile("<section[^>]*class=\"tab-pane\"[^>]*>").matcher(html).results()).hasSize(4);
    assertThat(Pattern.compile("<section[^>]*class=\"tab-pane\"[^>]*hidden").matcher(html).results()).isEmpty();
    assertThat(html).doesNotContain("navbar-user-chip");
    assertThat(html.indexOf("class=\"app-navbar\"")).isGreaterThan(html.indexOf("class=\"profile-header-card\""));
    assertUniqueIds(html);
    writePreview("dashboard-local", html);
    context.setVariable("displayName", "Very long display name ".repeat(12));
    context.setVariable("username", "very-long-address-".repeat(10) + "@example.com");
    writePreview("dashboard-long", templateEngine.process("dashboard", context));
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
        Map.of("id", "pk-1", "name", "MacBook Touch ID", "formattedCreatedAt", "Oct 01, 2026")
    ));

    String html = templateEngine.process("dashboard", context);

    assertThat(html).contains("Bob OAuth");
    assertThat(html).contains("No Local Password Set");
    assertThat(html).contains("Set Password");
    assertThat(html).contains("MacBook Touch ID");
    assertThat(html).doesNotContain(">Verified<");
    assertThat(html).contains("Added on Oct 01, 2026");
    assertThat(html).contains("Unlink");
    assertThat(html).contains("href=\"#tab-general\"");
    assertThat(html).doesNotContain("role=\"tablist\"");
    assertThat(html).contains("href=\"#tab-security\"");
    assertThat(html).contains("role=\"region\"");
    assertThat(html).contains("data-tab=\"tab-general\"");
    assertThat(html).contains("aria-labelledby=\"tab-btn-general\"");
    assertThat(html).contains("for=\"primaryEmailInput\"");
    assertThat(html).contains("for=\"avatarUrlInput\"");
    assertThat(html).contains("for=\"passkeyNameInput\"");
    assertThat(html).contains("id=\"avatarInitials\"");
    assertUniqueIds(html);
    writePreview("dashboard-oauth", html);
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

  @Test
  @DisplayName("register.html includes passkey-login script and accessible structure")
  void testRegisterIncludesPasskeyScript() {
    MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
    org.thymeleaf.context.IContext context = createWebContext(request);

    String html = templateEngine.process("register", context);

    assertThat(html).contains("src=\"/js/passkey-login.js\"");
    assertThat(html).contains("id=\"passkeyLoginBtn\"");
  }

  @Test
  @DisplayName("dashboard.html renders descriptive aria-labels for passkey remove and oauth unlink")
  void testDashboardDescriptiveAriaLabels() {
    MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
    org.thymeleaf.context.WebContext context = (org.thymeleaf.context.WebContext) createWebContext(request);
    context.setVariable("displayName", "Charlie Test");
    context.setVariable("username", "charlie@test.com");
    context.setVariable("avatarUrl", null);
    context.setVariable("provider", "LOCAL");
    context.setVariable("authorities", List.of(Map.of("authority", "ROLE_USER")));
    context.setVariable("hasPassword", true);
    context.setVariable("googleLinked", true);
    context.setVariable("githubLinked", true);
    context.setVariable("canUnlinkOAuth", true);
    context.setVariable("passkeys", List.of(
        Map.of("id", "pk-key-1", "name", "YubiKey 5C", "formattedCreatedAt", "Oct 05, 2026")
    ));

    String html = templateEngine.process("dashboard", context);

    assertThat(html).contains("aria-label=\"Remove passkey YubiKey 5C\"");
    assertThat(html).contains("aria-label=\"Unlink Google account\"");
    assertThat(html).contains("aria-label=\"Unlink GitHub account\"");
  }
  @Test
  @DisplayName("shared text fields escape values and associate helpers and validation messages")
  void testFieldSemanticsAndEscaping() {
    var context = (org.thymeleaf.context.WebContext) createWebContext(new MockHttpServletRequest(servletContext));
    context.setVariables(Map.of("id", "testField", "name", "testName", "label", "Test field",
        "value", "<script>alert(1)</script>", "autocomplete", "name", "required", true,
        "helper", "Helpful text", "error", "Please correct this field."));
    String html = templateEngine.process("fragments/ui/fields", Set.of("text"), context);
    assertThat(html).contains("for=\"testField\"", "aria-invalid=\"true\"",
        "aria-describedby=\"testField-hint testField-error\"", "id=\"testField-hint\"", "id=\"testField-error\"");
    assertThat(html).contains("&lt;script&gt;").doesNotContain("<script>");
    assertUniqueIds(html);
  }

  private void assertUniqueIds(String html) {
    var ids = Pattern.compile("\\bid=\"([^\"]+)\"").matcher(html).results().map(match -> match.group(1)).toList();
    assertThat(ids).doesNotHaveDuplicates();
  }

  // Render real templates for repeatable browser inspection without an authenticated service account.
  private void writePreview(String name, String html) {
    try {
      Path directory = Path.of("target", "ui-preview");
      Files.createDirectories(directory);
      Files.writeString(directory.resolve(name + ".html"), html);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

}
