package com.fitcoach.dto.response;

import com.fitcoach.domain.entity.TraineeFeedback;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** A single trainee-submitted feedback note, as shown on the coach's progress tab. */
@Data
@Builder
public class TraineeFeedbackResponse {
    private Long id;
    private String message;
    /** NUTRITION | WORKOUT | GENERAL */
    private String category;
    private LocalDateTime submittedAt;

    public static TraineeFeedbackResponse from(TraineeFeedback f) {
        return TraineeFeedbackResponse.builder()
                .id(f.getId())
                .message(f.getMessage())
                .category(f.getCategory() == null ? null : f.getCategory().name())
                .submittedAt(f.getCreatedAt())
                .build();
    }
}
