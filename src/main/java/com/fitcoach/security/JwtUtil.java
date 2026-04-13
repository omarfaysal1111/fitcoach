package com.fitcoach.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email, long issuedAtEpochSeconds) {
        return buildToken(email, issuedAtEpochSeconds);
    }

    private String buildToken(String subject, long issuedAtEpochSeconds) {
        Date issued = Date.from(Instant.ofEpochSecond(issuedAtEpochSeconds));
        Date expiry = new Date(issued.getTime() + jwtExpirationMs);
        return Jwts.builder()
                .subject(subject)
                .issuedAt(issued)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        return parseValidClaims(token).isPresent();
    }

    /**
     * Parses and verifies the token signature and expiry; empty if invalid.
     */
    public Optional<Claims> parseValidClaims(String token) {
        try {
            return Optional.of(parseClaims(token));
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
