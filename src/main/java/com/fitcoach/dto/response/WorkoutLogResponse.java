package com.fitcoach.dto.response;

import com.fitcoach.domain.enums.SetLogOutcome;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class WorkoutLogResponse {

    private Long completionId;
    private UUID planSessionId;
    private String planSessionTitle;
    /** Parent workout plan (template) for this session day. */
    private UUID workoutPlanId;
    private String workoutPlanTitle;
    /** Order of this day within the plan (1-based or as stored on {@link com.fitcoach.domain.entity.PlanSession}). */
    private Integer dayOrder;
    private String completionDate;
    private LocalDateTime completedAt;
    /** False when the trainee only used quick-complete with no per-exercise logs. */
    private boolean hasDetailedLogs;
    private List<ExerciseLogItem> exerciseLogs;

    @Data
    @Builder
    public static class ExerciseLogItem {
        private Long logId;
        private UUID planSessionExerciseId;
        private String exerciseName;
        private int plannedSets;
        private int actualSetsCompleted;
        private int skippedSets;
        private int missedSets;
        /** True when every prescribed set was logged as {@link SetLogOutcome#COMPLETED}. */
        private boolean allSetsCompleted;
        private String excuse;
        private String coachNotes;
        private Boolean isReviewedByCoach;
        private LocalDateTime loggedAt;
        /** Per-set detail; empty for legacy logs created before per-set tracking. */
        private List<SetLogDetail> setDetails;
    }

    @Data
    @Builder
    public static class SetLogDetail {
        private int setNumber;
        private SetLogOutcome outcome;
        private String reason;
        /** Weight lifted in kilograms for this set. Null when not recorded or bodyweight. */
        private Double weightKg;
        /** Actual reps performed for this set. Null when not recorded. */
        private Integer reps;
    }
}
