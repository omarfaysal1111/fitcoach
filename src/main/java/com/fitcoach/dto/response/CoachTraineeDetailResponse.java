package com.fitcoach.dto.response;

import com.fitcoach.dto.response.MealCompletionLogResponse;
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
    /**
     * Every recorded meal completion for this trainee (newest first). Each entry includes whether
     * the meal was skipped and any ingredient-level deviations (skipped or swapped ingredients).
     */
    private List<MealCompletionLogResponse> mealCompletionHistory;
    private Integer missedWorkoutsCount;
    private Integer missedMealsCount;
    private List<CoachGoalResponse> goals;
    private List<InBodyReportResponse> inbodyReports;
    private List<ProgressPhotoResponse> progressPhotos;
}
