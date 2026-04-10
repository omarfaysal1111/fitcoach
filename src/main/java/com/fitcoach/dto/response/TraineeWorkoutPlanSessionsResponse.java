package com.fitcoach.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Trainee-facing view: workout plan identity plus ordered plan sessions (days) only — no exercise lines.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraineeWorkoutPlanSessionsResponse {

    private UUID planId;
    private String planTitle;
    private String planDescription;

    /** Sessions ordered by {@code dayOrder} (e.g. Leg Day, Push Day). */
    private List<PlanSessionRow> sessions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanSessionRow {
        private UUID sessionId;
        private String title;
        private Integer dayOrder;
        private int exerciseCount;
    }
}
