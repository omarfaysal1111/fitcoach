package com.fitcoach.dto.response.plan;

import com.fitcoach.domain.entity.PlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response for {@link com.fitcoach.domain.entity.PlanAssignment}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanAssignmentResponse {

    private UUID id;
    private UUID planId;
    private Long traineeId;
    private LocalDate startDate;
    private PlanStatus status;
}
