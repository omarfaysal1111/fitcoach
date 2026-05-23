package com.fitcoach.dto.response.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Summary card shown in the admin "All Coaches" list.
 */
@Data
@Builder
public class AdminCoachSummaryResponse {
    private Long coachId;
    private String fullName;
    private String email;
    private String specialisation;
    private long totalTrainees;
    private long activeTrainees;
    private LocalDateTime memberSince;
}
