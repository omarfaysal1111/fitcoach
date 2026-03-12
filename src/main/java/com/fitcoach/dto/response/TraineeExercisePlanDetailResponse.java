package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TraineeExercisePlanDetailResponse {

    private Long id;
    private String title;
    private String subtitle;
    private String difficulty;
    private int exercisesTotal;
    private int durationMinutes;
    private int estimatedCalories;
    private int setsTotal;
    private String coachNote;
    private List<ExerciseItem> exercises;

    @Data
    @Builder
    public static class ExerciseItem {
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

