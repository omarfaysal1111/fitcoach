package com.fitcoach.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterCoachRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 80, message = "Full name must be at most 80 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Size(max = 120, message = "Specialisation must be at most 120 characters")
    private String specialisation;

    private String bio;
}
