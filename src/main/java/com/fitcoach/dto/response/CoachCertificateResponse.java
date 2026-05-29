package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CoachCertificateResponse {
    private Long id;
    private String title;
    private String issuingOrganization;
    private Integer issueYear;
    private String imageUrl;
}
