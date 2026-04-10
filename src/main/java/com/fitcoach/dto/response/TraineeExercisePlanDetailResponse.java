package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TraineeExercisePlanDetailResponse {

    private UUID id;
    private String title;
    private String subtitle;
    private String difficulty;
    private int exercisesTotal;
    private int durationMinutes;
    private int estimatedCalories;
    private int setsTotal;
    private String coachNote;
    /** All exercises in plan order: sessions by {@code dayOrder}, then by line {@code order}. */
    private List<ExerciseItem> exercises;

    /** One entry per plan day (e.g. "Leg Day") for tabs or section headers. */
    private List<SessionSummary> sessions;

    @Data
    @Builder
    public static class SessionSummary {
        private UUID sessionId;
        private String title;
        private Integer dayOrder;
        private int exerciseCount;
    }

    @Data
    @Builder
    public static class ExerciseItem {
        private UUID id;
        /** Plan session (workout day) this line belongs to */
        private UUID sessionId;
        private String sessionTitle;
        private Integer dayOrder;
        private int order;
        private String name;
        private int sets;
        private String reps;
        private String load;
        private String rest;
        private String muscleGroup;
        private String status; // not_started | in_progress | done
    }
}
