package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CoachTransformationResponse {
    private Long id;
    private String clientLabel;
    private String description;
    private String statsSummary;
    private String beforePhotoUrl;
    private String afterPhotoUrl;
}
