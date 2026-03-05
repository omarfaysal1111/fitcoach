package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CoachProfileResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String specialisation;
    private String bio;
    private int traineeCount;
    private LocalDateTime createdAt;
}
