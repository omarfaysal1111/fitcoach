package com.fitcoach.dto.response;

import com.fitcoach.domain.enums.GoalStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CoachGoalResponse {
    private Long id;
    private Long traineeId;
    private Long coachId;
    private String title;
    private String description;
    private GoalStatus status;
    private LocalDate targetDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
