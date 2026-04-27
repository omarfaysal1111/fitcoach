package com.fitcoach.dto.response;

import com.fitcoach.domain.enums.FileType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class InBodyReportResponse {
    private Long id;
    private Long traineeId;
    private String label;
    private FileType fileType;
    private String fileUrl;
    private LocalDate reportDate;
    private LocalDateTime uploadedAt;
}
