package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ProgressPictureResponse {

    private Long id;
    private LocalDate date;
    private String frontPictureUrl;
    private String sidePictureUrl;
    private String backPictureUrl;
    private String notes;
    private LocalDateTime uploadedAt;
}
