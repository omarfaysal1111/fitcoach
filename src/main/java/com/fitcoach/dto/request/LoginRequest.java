package com.fitcoach.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Role the user selected on the login screen ("coach" | "trainee").
     * Optional for backward compatibility; when present it must match the
     * account's actual role, otherwise login is rejected (SCRUM-88).
     */
    private String role;
}
