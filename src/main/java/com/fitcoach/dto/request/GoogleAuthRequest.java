package com.fitcoach.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class GoogleAuthRequest {

    /** The Google ID Token returned by the Google Sign-In SDK on the device. */
    @NotBlank(message = "idToken is required")
    private String idToken;

    /** "coach" or "trainee" — determines which profile table to create on first sign-in. */
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
