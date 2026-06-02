package com.fitcoach.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatNotifyRequest {
    /** Entity ID of the recipient (trainee.id or coach.id, not user.id) */
    @NotNull
    private Long recipientEntityId;
    /** "COACH" or "TRAINEE" — the role of the recipient */
    @NotBlank
    private String recipientRole;
    @NotBlank
    private String text;
    @NotBlank
    private String conversationId;
}
