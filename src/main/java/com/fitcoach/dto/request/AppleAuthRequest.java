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
     * Apple only sends the user's full name on the VERY FIRST sign-in.
     * The Flutter SDK extracts it and passes it here so we can persist it.
     * Null on all subsequent logins.
     */
    private String fullName;

    /** Required only when role = "trainee" AND the account does not yet exist. */
    private String invitationToken;
}
