package com.fitcoach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verifies Apple identity tokens (Sign in with Apple).
 *
 * <p>Apple tokens are standard JWTs signed with an RSA key whose public counterpart
 * is published at {@code https://appleid.apple.com/auth/keys} (JWKS format).
 * We fetch the matching key by {@code kid}, reconstruct the {@link PublicKey}, and let
 * jjwt 0.12.x validate the signature, expiry, issuer, and audience claims.
 *
 * <p><b>JWKS caching:</b> Apple rotates keys infrequently, so we cache the raw JWKS JSON
 * in memory and refresh it lazily only when an unknown {@code kid} is encountered.
 * This avoids an HTTP round-trip on every sign-in while staying correct if Apple adds keys.
 */
@Component
public class AppleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(AppleTokenVerifier.class);

    @Value("${app.apple.client-id}")
    private String expectedAudience;

    @Value("${app.apple.jwks-url}")
    private String jwksUrl;

    @Value("${app.apple.issuer}")
    private String expectedIssuer;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** In-memory cache of Apple's JWKS JSON (refreshed on unknown kid). */
    private final AtomicReference<String> cachedJwks = new AtomicReference<>();

    /**
     * Verifies the Apple identity token and returns its claims.
     *
     * @param identityToken raw JWT string from the device
     * @return verified {@link Claims} containing {@code sub}, {@code email}, etc.
     * @throws IllegalArgumentException if the token is invalid, expired, or audience/issuer mismatch
     */
    public Claims verify(String identityToken) {
        try {
            String kid = extractKid(identityToken);

            // Try with cached JWKS first; refresh once on cache miss
            PublicKey publicKey = resolvePublicKey(kid, false);
            if (publicKey == null) {
                log.info("Unknown Apple kid '{}', refreshing JWKS cache.", kid);
                publicKey = resolvePublicKey(kid, true);
            }
            if (publicKey == null) {
                throw new IllegalArgumentException(
                        "No matching Apple public key found for kid: " + kid);
            }

            return Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(expectedIssuer)
                    .requireAudience(expectedAudience)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();

        } catch (io.jsonwebtoken.JwtException e) {
            throw new IllegalArgumentException(
                    "Apple token is invalid or expired: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Apple token verification failed: " + e.getMessage(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractKid(String identityToken) throws Exception {
        String[] parts = identityToken.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Apple identity token is malformed");
        }
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        JsonNode headerNode = objectMapper.readTree(headerJson);
        JsonNode kidNode = headerNode.get("kid");
        if (kidNode == null) {
            throw new IllegalArgumentException("Apple identity token header is missing 'kid'");
        }
        return kidNode.asText();
    }

    private PublicKey resolvePublicKey(String kid, boolean forceRefresh) throws Exception {
        if (forceRefresh || cachedJwks.get() == null) {
            String fresh = restTemplate.getForObject(jwksUrl, String.class);
            cachedJwks.set(fresh);
        }
        String jwksJson = cachedJwks.get();
        if (jwksJson == null) return null;

        JsonNode keys = objectMapper.readTree(jwksJson).get("keys");
        if (keys == null || !keys.isArray()) return null;

        for (JsonNode key : keys) {
            if (kid.equals(key.get("kid").asText())) {
                BigInteger modulus  = new BigInteger(1,
                        Base64.getUrlDecoder().decode(key.get("n").asText()));
                BigInteger exponent = new BigInteger(1,
                        Base64.getUrlDecoder().decode(key.get("e").asText()));
                return KeyFactory.getInstance("RSA")
                        .generatePublic(new RSAPublicKeySpec(modulus, exponent));
            }
        }
        return null;
    }
}
