package com.example.authserver.service.passkey;

import com.example.authserver.controller.dto.FinishLoginRequest;
import com.example.authserver.model.AppUser;
import com.example.authserver.model.Authority;
import com.example.authserver.model.UserPasskey;
import com.example.authserver.repo.UserPasskeyRepository;
import com.example.authserver.service.JwtService;
import com.example.authserver.service.appuser.AppUserService;
import com.example.authserver.service.token.RedisTokenService;
import com.example.authserver.util.CookieUtil;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.webauthn.api.COSEAlgorithmIdentifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasskeyService {

  private static final String ACCESS_TOKEN_COOKIE = "access_token";
  private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
  private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
  private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

  private final AppUserService appUserService;
  private final UserPasskeyRepository passkeyRepository;
  private final StringRedisTemplate redisTemplate;
  private final JwtService jwtService;
  private final RedisTokenService redisTokenService;

  private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
  private final ObjectConverter objectConverter = new ObjectConverter();

  @Value("${app.webauthn.rp-id:localhost}")
  private String rpId;

  @Value("${app.webauthn.origin:http://localhost:9090}")
  private String origin;

  // 1. Generate Registration Request Options
  public Map<String, Object> startRegistration(String email) {
    log.info("Generating passkey registration options for user email: {}", email);

    final var user = appUserService.findByEmail(email)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + email));

    Challenge challenge = new DefaultChallenge();
    String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.getValue());

    // Cache challenge in Redis for 5 minutes bound to user
    redisTemplate.opsForValue().set("passkey_reg_challenge:" + user.getUsername(), challengeBase64, Duration.ofMinutes(5));

    final String displayName = (user.getFullName() != null && !user.getFullName().isBlank())
        ? user.getFullName()
        : user.getUsername();

    Map<String, Object> options = new HashMap<>();
    options.put("challenge", challengeBase64);
    options.put("rp", Map.of("name", "Spring Security Auth Server", "id", rpId));
    options.put("user", Map.of(
        "id", Base64.getUrlEncoder().withoutPadding().encodeToString(user.getUsername().getBytes()),
        "name", user.getUsername(),
        "displayName", displayName
    ));

    options.put("pubKeyCredParams", List.of(
        Map.of("type", "public-key", "alg", COSEAlgorithmIdentifier.ES256.getValue()),
        Map.of("type", "public-key", "alg", COSEAlgorithmIdentifier.RS256.getValue())
    ));
    options.put("authenticatorSelection", Map.of(
        "userVerification", "preferred",
        "residentKey", "preferred"
    ));
    options.put("timeout", 60000);

    return options;
  }

  // 2. Validate Attestation and Persist Credential
  @Transactional
  public void finishRegistration(String email, String credentialId, byte[] attestationObject, byte[] clientDataJSON, String label) {
    log.info("Validating passkey attestation and persisting credential for email: {}", email);

    final var user = appUserService.findByEmail(email)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + email));

    String cachedChallenge = redisTemplate.opsForValue().get("passkey_reg_challenge:" + user.getUsername());
    if (cachedChallenge == null) {
      log.warn("Registration challenge expired or missing for user: {}", user.getUsername());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration challenge expired");
    }

    ServerProperty serverProperty = ServerProperty.builder()
        .origin(Origin.create(origin))
        .rpId(rpId)
        .challenge(new DefaultChallenge(Base64.getUrlDecoder().decode(cachedChallenge)))
        .build();

    RegistrationRequest regRequest = new RegistrationRequest(attestationObject, clientDataJSON);
    RegistrationParameters regParams = new RegistrationParameters(serverProperty, false);

    RegistrationData regData = webAuthnManager.validate(regRequest, regParams);

    // Extract public key and counter
    final var authenticatorData = regData.getAttestationObject().getAuthenticatorData();
    final var attestedCredentialData = authenticatorData.getAttestedCredentialData();
    final byte[] publicKeyBytes = (attestedCredentialData != null && attestedCredentialData.getCOSEKey() != null)
        ? objectConverter.getCborConverter().writeValueAsBytes(attestedCredentialData.getCOSEKey())
        : new byte[0];
    final long signCount = authenticatorData.getSignCount();

    final UserPasskey passkey = UserPasskey.builder()
        .user(user)
        .credentialId(credentialId)
        .publicKeyCose(publicKeyBytes)
        .signCount(signCount)
        .label(label)
        .build();

    passkeyRepository.save(passkey);
    log.info("Successfully persisted passkey credential ID '{}' for user: {}", credentialId, user.getUsername());

    redisTemplate.delete("passkey_reg_challenge:" + user.getUsername());
  }

  // 3. Generate Login Assertion Options (Supports Username-less / Discoverable Credentials)
  public Map<String, Object> startLogin(String email) {
    log.info("Generating passkey login assertion options (email identifier: {})", email != null ? email : "none");

    Challenge challenge = new DefaultChallenge();
    String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.getValue());
    String challengeId = UUID.randomUUID().toString();

    // Cache challenge in Redis for 5 minutes by challengeId
    redisTemplate.opsForValue().set("passkey_login_challenge:" + challengeId, challengeBase64, Duration.ofMinutes(5));

    Map<String, Object> options = new HashMap<>();
    options.put("challengeId", challengeId);
    options.put("challenge", challengeBase64);
    options.put("rpId", rpId);
    options.put("userVerification", "preferred");
    options.put("timeout", 60000);

    // If an email was provided, optionally pre-fill allowedCredentials
    if (email != null && !email.isBlank()) {
      appUserService.findByEmail(email.trim()).ifPresent(user -> {
        redisTemplate.opsForValue().set("passkey_login_challenge:" + user.getUsername(), challengeBase64, Duration.ofMinutes(5));
        List<UserPasskey> passkeys = passkeyRepository.findAllByUser_Username(user.getUsername());
        if (!passkeys.isEmpty()) {
          List<Map<String, Object>> allowCredentials = passkeys.stream()
              .map(pk -> Map.<String, Object>of(
                  "type", "public-key",
                  "id", pk.getCredentialId()
              ))
              .toList();
          options.put("allowCredentials", allowCredentials);
        }
      });
    }

    return options;
  }

  // 4. Validate Assertion and Authenticate User (Username-less / Discoverable Supported)
  @Transactional
  public void finishLogin(String email,
                          FinishLoginRequest req,
                          HttpServletRequest request,
                          HttpServletResponse response) {
    log.info("Validating passkey assertion for credentialId: {}", req.credentialId());

    // 1. Resolve registered passkey from credentialId directly from DB
    final UserPasskey passkey = passkeyRepository.findByCredentialId(req.credentialId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Passkey credential not recognized"));

    final AppUser user = passkey.getUser();

    // 2. Resolve cached challenge by challengeId, username, or email fallback
    String cachedChallenge = null;
    if (req.challengeId() != null && !req.challengeId().isBlank()) {
      cachedChallenge = redisTemplate.opsForValue().get("passkey_login_challenge:" + req.challengeId());
    }
    if (cachedChallenge == null) {
      cachedChallenge = redisTemplate.opsForValue().get("passkey_login_challenge:" + user.getUsername());
    }
    if (cachedChallenge == null && email != null && !email.isBlank()) {
      cachedChallenge = redisTemplate.opsForValue().get("passkey_login_challenge:" + email.trim());
    }

    if (cachedChallenge == null) {
      log.warn("Passkey challenge expired or not found for user: {}", user.getUsername());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passkey challenge expired. Please try again.");
    }

    try {
      final COSEKey coseKey = objectConverter.getCborConverter().readValue(passkey.getPublicKeyCose(), COSEKey.class);

      final byte[] credentialIdBytes = Base64.getUrlDecoder().decode(req.credentialId());
      final byte[] clientDataJSONBytes = Base64.getUrlDecoder().decode(req.clientDataJSON());
      final byte[] authenticatorDataBytes = Base64.getUrlDecoder().decode(req.authenticatorData());
      final byte[] signatureBytes = Base64.getUrlDecoder().decode(req.signature());
      final byte[] userHandleBytes = (req.userHandle() != null && !req.userHandle().isBlank())
          ? Base64.getUrlDecoder().decode(req.userHandle())
          : null;

      final ServerProperty serverProperty = ServerProperty.builder()
          .origin(Origin.create(origin))
          .rpId(rpId)
          .challenge(new DefaultChallenge(Base64.getUrlDecoder().decode(cachedChallenge)))
          .build();

      final AttestedCredentialData attestedCredData = new AttestedCredentialData(
          AAGUID.ZERO,
          credentialIdBytes,
          coseKey
      );

      final AuthenticatorImpl authenticator = new AuthenticatorImpl(
          attestedCredData,
          null,
          passkey.getSignCount()
      );

      final AuthenticationRequest authRequest = new AuthenticationRequest(
          credentialIdBytes,
          userHandleBytes,
          authenticatorDataBytes,
          clientDataJSONBytes,
          signatureBytes
      );

      final AuthenticationParameters authParams = new AuthenticationParameters(
          serverProperty,
          authenticator,
          List.of(credentialIdBytes),
          false
      );

      final AuthenticationData authData = webAuthnManager.validate(authRequest, authParams);

      final long newSignCount = authData.getAuthenticatorData().getSignCount();
      passkey.setSignCount(newSignCount);
      passkeyRepository.save(passkey);

      // Clean up challenges from Redis
      if (req.challengeId() != null && !req.challengeId().isBlank()) {
        redisTemplate.delete("passkey_login_challenge:" + req.challengeId());
      }
      redisTemplate.delete("passkey_login_challenge:" + user.getUsername());

      issueAuthCookies(user, request, response);
      log.info("Passkey login completed successfully for user: {}", user.getUsername());

    } catch (ResponseStatusException rse) {
      throw rse;
    } catch (Exception ex) {
      log.error("Failed to validate passkey assertion for user: {}", user.getUsername(), ex);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Passkey authentication failed: " + ex.getMessage());
    }
  }

  private void issueAuthCookies(AppUser user, HttpServletRequest request, HttpServletResponse response) {
    final Set<String> roles = user.getAuthorities().stream()
        .map(Authority::getAuthority)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    if (roles.isEmpty()) {
      roles.add("ROLE_USER");
    }

    final String accessToken = jwtService.generateToken(
        user.getUsername(),
        user.getUsername(),
        roles,
        ACCESS_TOKEN_TTL.toMinutes()
    );

    final String refreshToken = redisTokenService.createRefreshToken(
        user.getUsername(),
        REFRESH_TOKEN_TTL
    );

    final boolean secure = request.isSecure();
    CookieUtil.attachCookie(response, ACCESS_TOKEN_COOKIE, accessToken, ACCESS_TOKEN_TTL.toSeconds(), secure);
    CookieUtil.attachCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, REFRESH_TOKEN_TTL.toSeconds(), secure);

    final List<SimpleGrantedAuthority> authorities = roles.stream()
        .map(SimpleGrantedAuthority::new)
        .toList();
    final UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
    final SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
    new HttpSessionSecurityContextRepository().saveContext(context, request, response);
  }
}