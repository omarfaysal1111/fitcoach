package com.fitcoach.dto.response.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full coach profile plus their trainee roster – used by the admin
 * "coach detail" screen.
 */
@Data
@Builder
public class AdminCoachDetailResponse {
    private Long coachId;
    private String fullName;
    private String email;
    private String specialisation;
    private String bio;
    private long totalTrainees;
    private long activeTrainees;
    private LocalDateTime memberSince;
    private List<AdminTraineeResponse> trainees;
}
