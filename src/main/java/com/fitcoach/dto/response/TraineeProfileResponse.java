package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TraineeProfileResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String fitnessGoal;
    private Long coachId;
    private String coachName;
    private LocalDateTime createdAt;
}
