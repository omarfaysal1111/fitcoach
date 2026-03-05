package com.fitcoach.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InvitationRequest {

    @NotBlank(message = "Invitee email is required")
    @Email(message = "Must be a valid email address")
    private String email;
}
