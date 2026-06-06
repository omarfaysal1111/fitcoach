package com.fitcoach.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Verifies Google ID Tokens using Google's official SDK.
 *
 * Accepts a comma-separated list of client IDs so both the web client (used by
 * the backend) and the iOS / Android native client IDs can be trusted. This is
 * required because google_sign_in on iOS issues tokens whose "aud" is the iOS
 * client ID, while Android (with serverClientId configured) issues tokens with
 * the web client ID as "aud".
 */
@Component
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(
            @Value("${app.google.client-ids:${app.google.client-id:}}") String clientIds) {

        List<String> audiences = Arrays.stream(clientIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        if (audiences.isEmpty()) {
            throw new IllegalStateException(
                    "No Google client IDs configured. Set app.google.client-ids in application.properties.");
        }

        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(audiences)
                .build();
    }

    /**
     * Verifies the Google ID token signature, expiry, and audience.
     *
     * @param idToken raw ID token string from the device
     * @return verified {@link GoogleIdToken.Payload}
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
