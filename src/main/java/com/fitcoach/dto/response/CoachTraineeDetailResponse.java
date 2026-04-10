package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CoachTraineeDetailResponse {
    private TraineeProfileResponse profile;
    private List<MeasurementLogResponse> recentMeasurements;
    private List<ProgressPictureResponse> recentPictures;
    /**
     * Every recorded plan-session completion for this trainee (newest first). Each entry includes
     * per-exercise and per-set outcomes (completed / skipped / missed) when the trainee used
     * complete-with-logs; otherwise {@link WorkoutLogResponse#isHasDetailedLogs()} is false.
     */
    private List<WorkoutLogResponse> workoutCompletionHistory;
    private Integer missedWorkoutsCount;
    private Integer missedMealsCount;
}
