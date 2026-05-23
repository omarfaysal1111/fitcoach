package com.fitcoach.dto.response.admin;

import com.fitcoach.domain.enums.TraineeStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Trainee record returned inside a coach's detail view or the standalone
 * trainee-list endpoint.
 */
@Data
@Builder
public class AdminTraineeResponse {
    private Long traineeId;
    private String fullName;
    private String email;
    private TraineeStatus status;
    private String fitnessGoal;
    private String traineeLevel;
    private int currentStreak;
    private LocalDateTime joinedAt;
}
