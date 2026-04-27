package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ProgressPhotoResponse {
    private Long id;
    private Long traineeId;
    private String label;
    private String fileUrl;
    private LocalDate photoDate;
    private LocalDateTime uploadedAt;
}
