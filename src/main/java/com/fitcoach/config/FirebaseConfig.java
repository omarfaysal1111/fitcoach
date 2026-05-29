package com.fitcoach.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.service-account-path:}")
    private String serviceAccountPath;

    @PostConstruct
    public void initialize() {
        if (!FirebaseApp.getApps().isEmpty()) return;

        try {
            GoogleCredentials credentials;
            if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
                try (InputStream is = new FileInputStream(serviceAccountPath)) {
                    credentials = GoogleCredentials.fromStream(is);
                }
            } else {
                // Falls back to GOOGLE_APPLICATION_CREDENTIALS env var
                credentials = GoogleCredentials.getApplicationDefault();
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized");
        } catch (IOException e) {
            log.warn("Firebase Admin SDK NOT initialized (push notifications disabled): {}", e.getMessage());
        }
    }
}
