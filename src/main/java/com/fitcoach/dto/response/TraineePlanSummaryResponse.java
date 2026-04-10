package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TraineePlanSummaryResponse {

    /** String so both numeric nutrition plan ids and UUID workout plan ids serialize cleanly. */
    private String id;
    private String title;
    private String description;
    private String type; // "EXERCISE" or "NUTRITION"
}
