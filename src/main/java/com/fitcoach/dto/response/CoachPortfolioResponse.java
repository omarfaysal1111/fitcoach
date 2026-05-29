package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CoachPortfolioResponse {
    private String coachId;
    private String coachName;
    private String specialisation;
    private String bio;
    private List<CoachCertificateResponse> certificates;
    private List<CoachTransformationResponse> transformations;
    private List<CoachPortfolioItemResponse> items;
}
