package com.fitcoach.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Verifies Google ID Tokens using Google's official SDK.
 * Validates signature, expiry, and audience (client ID) in one call.
 */
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
     * Verifies the Google ID token signature, expiry, and audience.
     *
     * @param idToken raw ID token string from the device
     * @return verified {@link GoogleIdToken.Payload} containing {@code sub}, {@code email}, {@code name}, etc.
     * @throws IllegalArgumentException if the token is invalid, expired, or audience does not match
     */
    public GoogleIdToken.Payload verify(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new IllegalArgumentException("Google ID token is invalid or expired");
            }
            return token.getPayload();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Google token verification failed: " + e.getMessage(), e);
        }
    }
}
