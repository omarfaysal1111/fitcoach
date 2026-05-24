# Backend Requirements — Google & Apple SSO
**Project:** fitcoach (Spring Boot 3 / PostgreSQL)  
**Authored:** 2026-05-24  
**Scope:** Two new public endpoints (`POST /auth/google`, `POST /auth/apple`) that verify a third-party identity token and return the same `AuthResponse` JWT the rest of the app already uses.

---

## 1. High-Level Flow

```
Flutter                         Backend                          External
  │                                │                                │
  │── idToken / identityToken ──►  │── verify token ──────────────►│ Google / Apple public keys
  │   + role                       │◄─ claims (email, name, sub) ──│
  │                                │
  │                                │── userRepository.findByEmail()
  │                                │   ┌── Found ──► issue new JWT, return AuthResponse
  │                                │   └── Not found ──► create User + Coach/Trainee
  │                                │                     issue JWT, return AuthResponse
  │◄── { data: { token } } ───────│
```

**The response shape is identical to the existing login endpoint** — `AuthResponse` wrapped in `ApiResponse`. No Flutter changes needed.

---

## 2. Business Rules

| Rule | Detail |
|---|---|
| **Coaches** can SSO freely | No invitation needed. A new account is auto-created on first SSO. |
| **Trainees** still require an invitation token on **first** SSO | Because a trainee must be linked to a coach. Pass `invitationToken` alongside the SSO payload. On subsequent logins (account already exists) the token is not required. |
| **Existing email collision** | If the email already exists as a **local** (password) account, reject SSO with `409 Conflict` and message: _"An account with this email already exists. Please log in with your password."_ |
| **Existing SSO re-login** | If the email + provider already exist, just issue a new JWT (same as `login`). |
| **Name fallback** | If Apple/Google returns a name, save it on first account creation. On re-login, do NOT overwrite `fullName`. |
| **Password field** | SSO users have no password. Store a `BCrypt`-hashed random `UUID` so the `NOT NULL` constraint is satisfied and the field can never be used to log in. |

---

## 3. Database Changes

### 3.1 `users` table — two new columns

```sql
ALTER TABLE users
    ADD COLUMN auth_provider  VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN provider_subject VARCHAR(255) NULL;

-- Prevent the same SSO subject from creating duplicate accounts
CREATE UNIQUE INDEX uq_users_provider_subject
    ON users (auth_provider, provider_subject)
    WHERE auth_provider <> 'LOCAL';
```

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `auth_provider` | `VARCHAR(20)` | `NOT NULL DEFAULT 'LOCAL'` | Enum values: `LOCAL`, `GOOGLE`, `APPLE` |
| `provider_subject` | `VARCHAR(255)` | `NULL` | Google `sub` claim or Apple `sub` claim (stable user identifier) |

### 3.2 JPA entity change — `User.java`

Add to the `User` entity:

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
@Builder.Default
private AuthProvider authProvider = AuthProvider.LOCAL;

@Column(length = 255)
private String providerSubject;
```

Create the enum:

```java
// com/fitcoach/domain/enums/AuthProvider.java
public enum AuthProvider { LOCAL, GOOGLE, APPLE }
```

---

## 4. New Maven Dependencies (`pom.xml`)

```xml
<!-- Google ID Token verification -->
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.7.0</version>
</dependency>

<!-- Apple Sign In — JWT verification via Apple's public JWKS -->
<!-- Uses the existing jjwt-api dependency already in pom.xml  -->
<!-- No additional library needed; Apple tokens are standard JWTs verified
     against Apple's JWKS endpoint (see AppleTokenVerifier below).         -->
```

> **Note:** `jjwt-api`, `jjwt-impl`, and `jjwt-jackson` are already present in `pom.xml` and are sufficient for Apple token verification.

---

## 5. New Application Properties (`application.properties`)

```properties
# ── Google SSO ────────────────────────────────────────────────────────────────
# OAuth 2.0 Client ID from Google Cloud Console
# (the same client ID embedded in google-services.json / GoogleService-Info.plist)
app.google.client-id=YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com

# ── Apple SSO ─────────────────────────────────────────────────────────────────
# Your Apple App ID / Bundle ID — used as the expected "aud" claim in Apple JWTs
app.apple.client-id=com.fitcoach.guider
# Apple's public key endpoint (do not change)
app.apple.jwks-url=https://appleid.apple.com/auth/keys
# Apple's issuer claim (do not change)
app.apple.issuer=https://appleid.apple.com
```

---

## 6. New DTOs

### 6.1 `GoogleAuthRequest.java`

```java
package com.fitcoach.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class GoogleAuthRequest {

    /** The Google ID Token returned by the Google Sign-In SDK on the device. */
    @NotBlank(message = "idToken is required")
    private String idToken;

    /** "coach" or "trainee" — determines which profile table to create. */
    @NotBlank(message = "role is required")
    @Pattern(regexp = "^(coach|trainee)$", flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "role must be 'coach' or 'trainee'")
    private String role;

    /**
     * Required only when role = "trainee" AND the account does not yet exist.
     * Ignored for coaches or returning SSO users.
     */
    private String invitationToken;
}
```

### 6.2 `AppleAuthRequest.java`

```java
package com.fitcoach.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AppleAuthRequest {

    /** The JWT identity token returned by Sign in with Apple. */
    @NotBlank(message = "identityToken is required")
    private String identityToken;

    /** The short-lived authorization code — reserved for server-side code exchange if needed. */
    @NotBlank(message = "authorizationCode is required")
    private String authorizationCode;

    /** "coach" or "trainee" */
    @NotBlank(message = "role is required")
    @Pattern(regexp = "^(coach|trainee)$", flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "role must be 'coach' or 'trainee'")
    private String role;

    /**
     * Apple only sends the user's name on the VERY FIRST sign-in.
     * The Flutter SDK extracts it and passes it here so we can persist it.
     * Null on all subsequent logins.
     */
    private String fullName;

    /** Required only when role = "trainee" AND the account does not yet exist. */
    private String invitationToken;
}
```

---

## 7. Token Verifier Services

### 7.1 `GoogleTokenVerifier.java`

```java
package com.fitcoach.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${app.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Verifies the token signature and audience.
     *
     * @return verified payload
     * @throws IllegalArgumentException if token is invalid or expired
     */
    public GoogleIdToken.Payload verify(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new IllegalArgumentException("Google ID token is invalid or expired");
            }
            return token.getPayload();
        } catch (Exception e) {
            throw new IllegalArgumentException("Google token verification failed: " + e.getMessage(), e);
        }
    }
}
```

### 7.2 `AppleTokenVerifier.java`

```java
package com.fitcoach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

@Component
public class AppleTokenVerifier {

    @Value("${app.apple.client-id}")
    private String expectedAudience;

    @Value("${app.apple.jwks-url}")
    private String jwksUrl;

    @Value("${app.apple.issuer}")
    private String expectedIssuer;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Verifies the Apple identity token and returns its claims.
     *
     * @throws IllegalArgumentException if token is invalid, expired, or audience mismatch
     */
    public Claims verify(String identityToken) {
        try {
            // 1. Fetch Apple's current public keys (JWKS)
            String jwksJson = restTemplate.getForObject(jwksUrl, String.class);
            JsonNode keys = objectMapper.readTree(jwksJson).get("keys");

            // 2. Extract the "kid" header from the token to find the matching key
            String[] parts = identityToken.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            String kid = objectMapper.readTree(headerJson).get("kid").asText();

            // 3. Build the matching RSA public key
            PublicKey publicKey = null;
            for (JsonNode key : keys) {
                if (kid.equals(key.get("kid").asText())) {
                    BigInteger modulus  = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("n").asText()));
                    BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("e").asText()));
                    publicKey = KeyFactory.getInstance("RSA")
                            .generatePublic(new RSAPublicKeySpec(modulus, exponent));
                    break;
                }
            }
            if (publicKey == null) {
                throw new IllegalArgumentException("No matching Apple public key found for kid: " + kid);
            }

            // 4. Parse & verify signature, expiry, issuer, audience
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)       // jjwt 0.12+
                    .requireIssuer(expectedIssuer)
                    .requireAudience(expectedAudience)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();

            return claims;

        } catch (io.jsonwebtoken.JwtException e) {
            throw new IllegalArgumentException("Apple token is invalid or expired: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Apple token verification failed: " + e.getMessage(), e);
        }
    }
}
```

> **JWKS Caching (recommended):** Apple's public keys rotate infrequently but the endpoint should not be called on every request in production. Add a `@Cacheable("appleJwks")` with a TTL of 1 hour using Spring's `CaffeineCacheManager` or Redis.

---

## 8. Updated `AuthService.java` — Two New Methods

Add these two methods inside the existing `AuthService` class. Also inject the two new verifier beans.

```java
// ── Inject new beans ──────────────────────────────────────────────────────────
private final GoogleTokenVerifier googleTokenVerifier;
private final AppleTokenVerifier  appleTokenVerifier;

// ── Google SSO ────────────────────────────────────────────────────────────────
@Transactional
public AuthResponse loginWithGoogle(GoogleAuthRequest request) {

    // 1. Verify the Google ID token — throws on failure
    GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());

    String email    = payload.getEmail();
    String name     = (String) payload.get("name");   // may be null
    String subject  = payload.getSubject();           // stable Google user ID

    return handleSsoLogin(
        email, name, subject,
        AuthProvider.GOOGLE,
        request.getRole(),
        request.getInvitationToken()
    );
}

// ── Apple SSO ─────────────────────────────────────────────────────────────────
@Transactional
public AuthResponse loginWithApple(AppleAuthRequest request) {

    // 1. Verify Apple identity token — throws on failure
    Claims claims = appleTokenVerifier.verify(request.getIdentityToken());

    String email   = claims.get("email", String.class);   // null on re-logins
    String subject = claims.getSubject();                  // stable Apple user ID

    // Apple only provides email on first sign-in.
    // For returning users: look up by provider_subject instead.
    return handleSsoLogin(
        email, request.getFullName(), subject,
        AuthProvider.APPLE,
        request.getRole(),
        request.getInvitationToken()
    );
}

// ── Shared SSO logic ──────────────────────────────────────────────────────────
private AuthResponse handleSsoLogin(
        String email,
        String fullName,
        String providerSubject,
        AuthProvider provider,
        String roleStr,
        String invitationToken) {

    // 1. Try to find by provider subject (most stable — Apple email can be hidden)
    User user = userRepository.findByAuthProviderAndProviderSubject(provider, providerSubject)
            .orElse(null);

    // 2. If not found by subject, try by email (handles first-time Apple with visible email)
    if (user == null && email != null) {
        user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            if (user.getAuthProvider() == AuthProvider.LOCAL) {
                // Clash: existing password account — reject
                throw new ConflictException(
                    "An account with this email already exists. Please log in with your password."
                );
            }
            // Same SSO provider, different session — backfill subject
            user.setProviderSubject(providerSubject);
        }
    }

    if (user != null) {
        // ── RE-LOGIN path: just issue a fresh JWT ────────────────────────────
        long iatSec = Instant.now().getEpochSecond();
        user.setJwtIssuedEpochSec(iatSec);
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), iatSec);
        return buildAuthResponse(token, user);
    }

    // ── FIRST-TIME REGISTRATION path ─────────────────────────────────────────
    if (email == null) {
        throw new IllegalArgumentException(
            "Cannot create account: email was not returned by the identity provider. " +
            "Please allow email access when signing in."
        );
    }
    if (userRepository.existsByEmail(email)) {
        throw new ConflictException(
            "An account with this email already exists. Please log in with your password."
        );
    }

    Role role = parseRole(roleStr);

    User newUser = User.builder()
            .fullName(fullName != null ? fullName : email.split("@")[0])
            .email(email)
            .password(passwordEncoder.encode(UUID.randomUUID().toString())) // unusable password
            .role(role)
            .authProvider(provider)
            .providerSubject(providerSubject)
            .build();
    userRepository.save(newUser);

    if (role == Role.COACH) {
        Coach coach = Coach.builder().user(newUser).build();
        coachRepository.save(coach);
        subscriptionService.initTrial(coach);                   // 7-day trial

    } else { // TRAINEE — invitation still required for first-time registration
        if (invitationToken == null || invitationToken.isBlank()) {
            throw new BadRequestException(
                "Trainees must provide an invitationToken to create an account via SSO."
            );
        }
        Coach coach = invitationService.consumeInvitation(invitationToken, email);
        Trainee trainee = Trainee.builder()
                .user(newUser)
                .coach(coach)
                .build();
        traineeRepository.save(trainee);
    }

    long iatSec = Instant.now().getEpochSecond();
    newUser.setJwtIssuedEpochSec(iatSec);
    userRepository.save(newUser);
    String token = jwtUtil.generateToken(newUser.getEmail(), iatSec);
    return buildAuthResponse(token, newUser);
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private Role parseRole(String roleStr) {
    return switch (roleStr.toLowerCase()) {
        case "coach"   -> Role.COACH;
        case "trainee" -> Role.TRAINEE;
        default        -> throw new IllegalArgumentException("Unknown role: " + roleStr);
    };
}
```

---

## 9. Updated `AuthController.java` — Two New Endpoints

```java
/**
 * POST /api/auth/google
 * Public – verify Google ID Token and return app JWT.
 */
@PostMapping("/google")
public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(
        @Valid @RequestBody GoogleAuthRequest request) {
    AuthResponse response = authService.loginWithGoogle(request);
    return ResponseEntity.ok(ApiResponse.ok("Google sign-in successful", response));
}

/**
 * POST /api/auth/apple
 * Public – verify Apple identity token and return app JWT.
 */
@PostMapping("/apple")
public ResponseEntity<ApiResponse<AuthResponse>> loginWithApple(
        @Valid @RequestBody AppleAuthRequest request) {
    AuthResponse response = authService.loginWithApple(request);
    return ResponseEntity.ok(ApiResponse.ok("Apple sign-in successful", response));
}
```

---

## 10. `SecurityConfig.java` — Permit New Routes

Add these two lines inside `authorizeHttpRequests`, **alongside the other public auth routes**:

```java
.requestMatchers(HttpMethod.POST, "/auth/google").permitAll()
.requestMatchers(HttpMethod.POST, "/auth/apple").permitAll()
```

---

## 11. `UserRepository.java` — New Query Method

```java
Optional<User> findByAuthProviderAndProviderSubject(AuthProvider provider, String providerSubject);
```

---

## 12. Error Handling

All exceptions must follow the existing `ApiResponse` error envelope. Expected HTTP responses:

| Scenario | HTTP Status | `message` |
|---|---|---|
| Invalid / expired Google token | `401 Unauthorized` | `"Google token is invalid or expired"` |
| Invalid / expired Apple token | `401 Unauthorized` | `"Apple token is invalid or expired"` |
| Email belongs to a LOCAL account | `409 Conflict` | `"An account with this email already exists. Please log in with your password."` |
| Trainee SSO without `invitationToken` (first time) | `400 Bad Request` | `"Trainees must provide an invitationToken to create an account via SSO."` |
| Apple token has no email (re-login but no account yet) | `400 Bad Request` | `"Cannot create account: email was not returned by the identity provider."` |
| `role` not `coach` or `trainee` | `400 Bad Request` | `"role must be 'coach' or 'trainee'"` |

Create a `BadRequestException` (if not already present):

```java
// com/fitcoach/exception/BadRequestException.java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
```

Ensure the global `@ControllerAdvice` handler maps:
- `IllegalArgumentException` → `400`
- `BadRequestException` → `400`
- `ConflictException` → `409` *(already exists)*

---

## 13. API Contract (for Postman / Flutter reference)

### `POST /api/auth/google`

**Request**
```json
{
  "idToken": "<Google ID Token from device>",
  "role": "coach"
}
```
*For a first-time trainee:*
```json
{
  "idToken": "<Google ID Token>",
  "role": "trainee",
  "invitationToken": "abc123xyz"
}
```

**Response `200 OK`**
```json
{
  "status": "success",
  "message": "Google sign-in successful",
  "data": {
    "token": "<app JWT>",
    "tokenType": "Bearer",
    "userId": 42,
    "fullName": "Omar Faysal",
    "email": "omar@gmail.com",
    "role": "COACH"
  }
}
```

---

### `POST /api/auth/apple`

**Request (first sign-in — Apple provides email + name)**
```json
{
  "identityToken": "<Apple identity JWT>",
  "authorizationCode": "<Apple auth code>",
  "role": "coach",
  "fullName": "Omar Faysal"
}
```
**Request (subsequent sign-ins — Apple sends no email/name)**
```json
{
  "identityToken": "<Apple identity JWT>",
  "authorizationCode": "<Apple auth code>",
  "role": "coach",
  "fullName": null
}
```

**Response `200 OK`** — same shape as Google response above.

---

## 14. Implementation Checklist

- [ ] Add `google-api-client` dependency to `pom.xml`
- [ ] Run DB migration: add `auth_provider`, `provider_subject` columns + unique index
- [ ] Add `AuthProvider` enum
- [ ] Update `User` entity with two new fields
- [ ] Add `app.google.client-id`, `app.apple.*` properties
- [ ] Create `GoogleTokenVerifier` component
- [ ] Create `AppleTokenVerifier` component (with JWKS caching)
- [ ] Add `BadRequestException` if not already present
- [ ] Add two new methods to `AuthService` (`loginWithGoogle`, `loginWithApple`, `handleSsoLogin`)
- [ ] Add two new endpoints to `AuthController`
- [ ] Permit `/auth/google` and `/auth/apple` in `SecurityConfig`
- [ ] Add `findByAuthProviderAndProviderSubject` to `UserRepository`
- [ ] Add error mappings to global `@ControllerAdvice`
- [ ] Test with Postman using a real Google ID Token (from device or OAuth Playground)
- [ ] Test Apple flow (requires real device with Apple ID — simulator not supported)
