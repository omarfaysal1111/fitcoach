package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CoachPortfolioItemResponse {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
}
