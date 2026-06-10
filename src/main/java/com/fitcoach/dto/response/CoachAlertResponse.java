package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CoachAlertResponse {
    private String id;
    private String traineeId;
    private String traineeName;
    private String message;
    /** "missed" | "nutrition" | "combined" */
    private String type;
    /** Combined adherence percentage (0–100) shown next to the trainee name. */
    private int adherencePct;
}
