package com.fitcoach.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class RegisterTraineeRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 80, message = "Full name must be at most 80 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Invitation token is required")
    private UUID invitationToken;

    @Size(max = 30, message = "Fitness goal must be at most 30 characters")
    private String fitnessGoal;
}
