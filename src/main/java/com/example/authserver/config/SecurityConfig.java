package com.example.authserver.config;

import com.example.authserver.repo.RememberMeTokenRepo;
import com.example.authserver.security.filter.JwtAuthenticationFilter;
import com.example.authserver.security.handler.FormLoginAuthenticationFailureHandler;
import com.example.authserver.service.oauth.CustomOAuth2UserService;
import com.example.authserver.service.oauth.OAuth2AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
public class SecurityConfig {
  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }


  @Bean
  @Order(1)
  SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                             JwtAuthenticationFilter jwtAuthFilter) throws Exception {
    http
        .securityMatcher("/api/**")
        .csrf(CsrfConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**", "/api/avatar/**", "/api/passkeys/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
    http
        .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        .with(authorizationServerConfigurer, (authorizationServer) ->
            authorizationServer
                .oidc(Customizer.withDefaults())
        )
        .authorizeHttpRequests((authorize) ->
            authorize.anyRequest().authenticated()
        )

        // Redirect unauthenticated requests hitting /oauth2/authorize to the login form
        .exceptionHandling((exceptions) -> exceptions
            .defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
            )
        );

    return http.build();
  }

  @Bean
  @Order(3)
  public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter,
                                                 CustomOAuth2UserService oAuth2UserService,
                                                 OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler,
                                                 FormLoginAuthenticationFailureHandler formLoginFailureHandler,
                                                 RememberMeServices rememberMeServices) throws Exception {
    http
        .csrf(CsrfConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/avatar/**", "/login/**", "/register", "/css/**", "/js/**", "/favicon.ico", "/oauth2/**").permitAll()
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .formLogin(form ->
            form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", false)
                .failureHandler(formLoginFailureHandler)
                .permitAll()
        )
        .rememberMe(remember -> remember
            .rememberMeServices(rememberMeServices))
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout")
            .deleteCookies("access_token", "refresh_token", "accessToken", "remember-me")
            .permitAll()
        )
        .oauth2Login(oauth -> oauth
            .loginPage("/login")
            .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
            .successHandler(oAuth2SuccessHandler)
            .failureUrl("/login?error=oauth")
        ).addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  RememberMeServices rememberMeServices(UserDetailsService userDetailsService,
                                        @Value("${security.remember-me.key:auth-server-remember-me-secret-key}") String rememberMeKey, RememberMeTokenRepo persistentTokenRepository) {

    final var rememberMe = new PersistentTokenBasedRememberMeServices(rememberMeKey, userDetailsService, persistentTokenRepository);
    rememberMe.setTokenValiditySeconds(30 * 24 * 60 * 60);
    return rememberMe;
  }
}
