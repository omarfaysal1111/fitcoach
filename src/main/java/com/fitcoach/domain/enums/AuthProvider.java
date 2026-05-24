package com.fitcoach.domain.enums;

/**
 * Identifies how a user account was created / how they authenticate.
 * LOCAL  — classic email + password
 * GOOGLE — Google Sign-In (ID Token flow)
 * APPLE  — Sign in with Apple (identity token flow)
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    APPLE
}
