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
    private String type;
}
