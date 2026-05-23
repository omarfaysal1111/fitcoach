package com.fitcoach.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP service. Generates a 6-digit code, stores it with a 5-minute
 * TTL, and validates submissions.
 *
 * TODO: Replace the log.info delivery line with a real email/SMS sender once
 *       spring-boot-starter-mail (or Twilio SDK) is on the classpath.
 */
@Slf4j
@Service
public class OtpService {

    /** OTP lifetime in seconds (5 minutes). */
    private static final long OTP_TTL_SECONDS = 300;

    private record OtpEntry(String code, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    // ── Send ─────────────────────────────────────────────────────────────────

    /**
     * Generate and "send" a fresh OTP for the given email.
     * Any previous pending code for that email is overwritten.
     *
     * @param email target email address
     * @return the generated OTP code (return value is for testing convenience;
     *         in production the code should only leave via the delivery channel)
     */
    public String sendOtp(String email) {
        String code = generateCode();
        Instant expiresAt = Instant.now().plusSeconds(OTP_TTL_SECONDS);
        store.put(email.toLowerCase(), new OtpEntry(code, expiresAt));

        // ── Delivery ──────────────────────────────────────────────────────────
        // Replace this log line with your email/SMS integration:
        //   mailService.sendOtpEmail(email, code);
        //   smsService.sendOtp(phoneNumber, code);
        log.info("[OTP] code={} email={} expiresAt={}", code, email, expiresAt);

        return code;
    }

    // ── Verify ────────────────────────────────────────────────────────────────

    /**
     * Validate the supplied code.
     * The entry is removed on a successful match (one-time use).
     *
     * @param email address the OTP was sent to
     * @param code  6-digit code supplied by the user
     * @return true if valid and not expired; false otherwise
     */
    public boolean verifyOtp(String email, String code) {
        OtpEntry entry = store.get(email.toLowerCase());
        if (entry == null || entry.isExpired()) {
            store.remove(email.toLowerCase());
            return false;
        }
        if (!entry.code().equals(code)) {
            return false;
        }
        store.remove(email.toLowerCase()); // consume – one-time use
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateCode() {
        int n = random.nextInt(900_000) + 100_000; // always 6 digits
        return String.valueOf(n);
    }
}
