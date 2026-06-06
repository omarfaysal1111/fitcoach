package com.fitcoach.controller;

import com.fitcoach.domain.entity.NutritionPlan;
import com.fitcoach.domain.entity.WorkoutPlan;
import com.fitcoach.dto.request.ExtraMealRequest;
import com.fitcoach.dto.request.MealCompletionRequest;
import com.fitcoach.dto.request.TraineeNoteRequest;
import com.fitcoach.dto.request.TraineeOnboardingRequest;
import com.fitcoach.dto.request.UpdateTraineeRequest;
import com.fitcoach.dto.request.WaterIntakeUpsertRequest;
import com.fitcoach.dto.response.CoachPortfolioResponse;
import com.fitcoach.dto.response.ExtraMealLogResponse;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.CoachProfileResponse;
import com.fitcoach.dto.response.InBodyReportResponse;
import com.fitcoach.dto.response.IngredientResponse;
import com.fitcoach.dto.response.MealDetailedResponse;
import com.fitcoach.dto.response.NutritionPlanDetailedResponse;
import com.fitcoach.dto.response.ProgressPhotoResponse;
import com.fitcoach.dto.response.TraineeDashboardTodayResponse;
import com.fitcoach.dto.response.TraineeExercisePlanDetailResponse;
import com.fitcoach.dto.response.TraineePlanSummaryResponse;
import com.fitcoach.dto.response.TraineeWorkoutPlanSessionsResponse;
import com.fitcoach.dto.response.TraineeProfileResponse;
import com.fitcoach.dto.response.UploadItemResult;
import com.fitcoach.dto.response.WaterIntakeResponse;
import com.fitcoach.service.CoachPortfolioService;
import com.fitcoach.service.ExercisePlanService;
import com.fitcoach.service.InBodyReportService;
import com.fitcoach.service.NutritionPlanService;
import com.fitcoach.service.ProgressPhotoService;
import com.fitcoach.service.TraineeService;
import com.fitcoach.service.WaterIntakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trainees")
@RequiredArgsConstructor
public class TraineeController {

    private final TraineeService traineeService;
    private final NutritionPlanService nutritionPlanService;
    private final ExercisePlanService exercisePlanService;
    private final ProgressPhotoService progressPhotoService;
    private final InBodyReportService inBodyReportService;
    private final WaterIntakeService waterIntakeService;
    private final CoachPortfolioService coachPortfolioService;

    /** POST /api/trainees/me/onboarding – submit onboarding form (measurements, health, fitness level) */
    @PostMapping("/me/onboarding")
    public ResponseEntity<ApiResponse<TraineeProfileResponse>> completeOnboarding(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody TraineeOnboardingRequest request) {
        if (principal == null) throw new AuthenticationCredentialsNotFoundException("Unauthenticated");
        return ResponseEntity.ok(ApiResponse.ok(traineeService.completeOnboarding(principal.getUsername(), request)));
    }

    /** GET /api/trainees/me – authenticated trainee's own profile */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TraineeProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("Unauthenticated");
        }
        return ResponseEntity.ok(ApiResponse.ok(traineeService.getMyProfile(principal.getUsername())));
    }

    /** GET /api/trainees/me/dashboard-today – aggregated view for trainee home */
    @GetMapping("/me/dashboard-today")
    public ResponseEntity<ApiResponse<TraineeDashboardTodayResponse>> getMyTodayDashboard(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(traineeService.getTodayDashboard(principal.getUsername())));
    }

    /**
     * PUT /api/trainees/me/water-intake
     * Set total water consumed for a calendar day (liters). Replaces any previous value for that date.
     * {@code date} defaults to today if omitted.
     */
    @PutMapping("/me/water-intake")
    public ResponseEntity<ApiResponse<WaterIntakeResponse>> upsertMyWaterIntake(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody WaterIntakeUpsertRequest request) {
        WaterIntakeResponse body = waterIntakeService.upsertForTrainee(principal.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Water intake saved", body));
    }

    /**
     * GET /api/trainees/me/water-intake?date=yyyy-MM-dd
     * Returns liters for that day (0 if not logged). {@code date} defaults to today.
     */
    @GetMapping("/me/water-intake")
    public ResponseEntity<ApiResponse<WaterIntakeResponse>> getMyWaterIntake(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(
                ApiResponse.ok(waterIntakeService.getForTrainee(principal.getUsername(), date)));
    }

    /** POST /api/trainees/me/plan-sessions/{planSessionId}/complete – mark a plan session (plan day) completed today */
    @PostMapping("/me/plan-sessions/{planSessionId}/complete")
    public ResponseEntity<ApiResponse<String>> completePlanSession(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID planSessionId) {
        traineeService.completePlanSession(principal.getUsername(), planSessionId);
        return ResponseEntity.ok(ApiResponse.ok("Plan session completed"));
    }

    /** POST /api/trainees/me/meals/{mealId}/complete – mark a meal as completed/logged today */
/** POST /api/trainees/me/meals/{mealId}/complete – mark a meal as completed, skipped, or modified today */
    @PostMapping("/me/meals/{mealId}/complete")
    public ResponseEntity<ApiResponse<String>> completeMeal(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long mealId,
            @RequestBody(required = false) MealCompletionRequest request) {
        
        // If the request is null (no body sent), initialize a default one (perfect completion)
        if (request == null) {
            request = new MealCompletionRequest();
        }

        // Pass the request down to the service layer to handle the business logic
        traineeService.completeMeal(principal.getUsername(), mealId, request);
        
        String message = request.isSkipMeal() ? "Meal skipped successfully" : "Meal logged successfully";
        return ResponseEntity.ok(ApiResponse.ok(message));
    }    /** POST /api/trainees/me/avatar – upload trainee profile picture */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TraineeProfileResponse>> uploadAvatar(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("file") MultipartFile file) {
        if (principal == null) throw new AuthenticationCredentialsNotFoundException("Unauthenticated");
        return ResponseEntity.ok(ApiResponse.ok("Avatar uploaded", traineeService.uploadAvatar(principal.getUsername(), file)));
    }

    /** PUT /api/trainees/me – update profile */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<TraineeProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateTraineeRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Profile updated", traineeService.updateMyProfile(principal.getUsername(), request)));
    }

    /** GET /api/trainees/coach – view the assigned coach's profile */
    @GetMapping("/coach")
    public ResponseEntity<ApiResponse<CoachProfileResponse>> getMyCoach(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(traineeService.getMyCoach(principal.getUsername())));
    }

    /** GET /api/trainees/coach/portfolio – view the assigned coach's portfolio */
    @GetMapping("/coach/portfolio")
    public ResponseEntity<ApiResponse<CoachPortfolioResponse>> getMyCoachPortfolio(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                coachPortfolioService.getPortfolioForTrainee(principal.getUsername())));
    }
    
    /** GET /api/trainees/me/nutrition-plans – get my assigned nutrition plans (summaries only) */

@GetMapping("/me/nutrition-plans")
@Transactional(readOnly = true) // Ensures lazy-loaded meals and ingredients can be accessed within the request
public ResponseEntity<ApiResponse<List<NutritionPlanDetailedResponse>>> getMyNutritionPlans(
        @AuthenticationPrincipal UserDetails principal) {
    Long traineeId = traineeService.getTraineeByEmail(principal.getUsername()).getId();
    List<NutritionPlan> plans = nutritionPlanService.getPlansByTrainee(traineeId);

    // Pick the latest plan by createdAt (if any), null-safe
    List<NutritionPlanDetailedResponse> detailedPlans = plans.stream()
            .max(java.util.Comparator.comparing(
                    NutritionPlan::getCreatedAt,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
            .map(p -> List.of(NutritionPlanDetailedResponse.builder()
                    .id(String.valueOf(p.getId()))
                    .title(p.getTitle())
                    .description(p.getDescription())
                    .type("NUTRITION")
                    .meals(p.getMeals().stream()
                            .map(meal -> {
                                List<com.fitcoach.domain.entity.Ingredient> ingList =
                                        new java.util.ArrayList<>(meal.getIngredients());
                                double sumCalories = ingList.stream().mapToDouble(i -> i.getCalories() != null ? i.getCalories() : 0.0).sum();
                                double sumProtein  = ingList.stream().mapToDouble(i -> i.getProtein() != null ? i.getProtein() : 0.0).sum();
                                double sumCarbs    = ingList.stream().mapToDouble(i -> i.getCarbohydrates() != null ? i.getCarbohydrates() : 0.0).sum();
                                double sumFat      = ingList.stream().mapToDouble(i -> i.getFat() != null ? i.getFat() : 0.0).sum();
                                return MealDetailedResponse.builder()
                                    .id(String.valueOf(meal.getId()))
                                    .name(meal.getName())
                                    .calories(meal.getCalories() != null ? meal.getCalories() : sumCalories)
                                    .protein(meal.getProtein()  != null ? meal.getProtein()  : sumProtein)
                                    .carbs(meal.getCarbs()      != null ? meal.getCarbs()     : sumCarbs)
                                    .fat(meal.getFat()          != null ? meal.getFat()       : sumFat)
                                    .ingredients(meal.getIngredients().stream()
                                            .map(ingredient -> IngredientResponse.builder()
                                                    .id(ingredient.getId())
                                                    .name(ingredient.getName())
                                                    .servingQuantityG(ingredient.getServingQuantityG())
                                                    .calories(ingredient.getCalories())
                                                    .fat(ingredient.getFat())
                                                    .water(ingredient.getWater())
                                                    .carbohydrates(ingredient.getCarbohydrates())
                                                    .protein(ingredient.getProtein())
                                                    .totalMinerals(ingredient.getTotalMinerals())
                                                    .build())
                                            .toList())
                                    .build();
                            })
                            .toList())
                    .build()))
            .orElseGet(List::of);

    return ResponseEntity.ok(
            ApiResponse.ok("Nutrition plan retrieved successfully", detailedPlans));
}
    /** GET /api/trainees/me/exercise-plans – get my assigned exercise plans (summaries only) */
    @GetMapping("/me/exercise-plans")
    public ResponseEntity<ApiResponse<List<TraineePlanSummaryResponse>>> getMyExercisePlans(
            @AuthenticationPrincipal UserDetails principal) {
        Long traineeId = traineeService.getTraineeByEmail(principal.getUsername()).getId();
        List<WorkoutPlan> plans = exercisePlanService.getPlansByTrainee(traineeId);

        List<TraineePlanSummaryResponse> summaries = plans.stream()
                .max(java.util.Comparator.comparing(
                        WorkoutPlan::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(p -> List.of(TraineePlanSummaryResponse.builder()
                        .id(p.getId().toString())
                        .title(p.getTitle())
                        .description(p.getDescription())
                        .type("EXERCISE")
                        .build()))
                .orElseGet(List::of);

        return ResponseEntity.ok(
                ApiResponse.ok("Exercise plans retrieved successfully", summaries));
    }

    /** GET /api/trainees/me/exercise-plans/{planId} – detailed view of a specific plan for the trainee */
    @GetMapping("/me/exercise-plans/{planId}")
    public ResponseEntity<ApiResponse<TraineeExercisePlanDetailResponse>> getMyExercisePlanDetail(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID planId) {
        TraineeExercisePlanDetailResponse detail =
                traineeService.getExercisePlanDetail(principal.getUsername(), planId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    /**
     * POST /api/trainees/me/extra-meals
     * Log an ad-hoc meal that is not part of the assigned nutrition plan.
     * Either {@code ingredientId} (resolves name automatically) or a free-text {@code name} is required.
     */
    @PostMapping("/me/extra-meals")
    public ResponseEntity<ApiResponse<ExtraMealLogResponse>> logExtraMeal(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ExtraMealRequest request) {
        ExtraMealLogResponse response = traineeService.logExtraMeal(principal.getUsername(), request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.ok("Extra meal logged", response));
    }

    /** GET /api/trainees/me/exercise-plans/{planId}/sessions – plan sessions only (no exercise lines) */
    @GetMapping("/me/exercise-plans/{planId}/sessions")
    public ResponseEntity<ApiResponse<TraineeWorkoutPlanSessionsResponse>> getMyWorkoutPlanSessions(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID planId) {
        TraineeWorkoutPlanSessionsResponse body =
                traineeService.getWorkoutPlanSessions(principal.getUsername(), planId);
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    /** POST /api/trainees/me/notes – trainee sends a note to their coach */
    @PostMapping("/me/notes")
    public ResponseEntity<ApiResponse<Void>> sendNoteToCoach(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody TraineeNoteRequest request) {
        if (request.getNote() == null || request.getNote().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Note cannot be empty"));
        }
        traineeService.sendNoteToCoach(principal.getUsername(), request.getNote());
        return ResponseEntity.ok(ApiResponse.ok("Note sent to coach", null));
    }

    /** POST /api/trainees/me/progress-photos – trainee uploads one or more progress photos */
    @PostMapping(value = "/me/progress-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<UploadItemResult<ProgressPhotoResponse>>>> uploadMyProgressPhoto(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "picture", required = false) MultipartFile picture,
            @RequestParam(value = "label", required = false) String label,
            @RequestParam(value = "photoDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate photoDate) {
        List<MultipartFile> toProcess = resolveFiles(files, file, photo, image, picture);
        List<UploadItemResult<ProgressPhotoResponse>> results = toProcess.stream()
                .map(f -> {
                    try {
                        ProgressPhotoResponse r = progressPhotoService.uploadPhotoByTrainee(
                                principal.getUsername(), f, label, photoDate);
                        return UploadItemResult.<ProgressPhotoResponse>builder()
                                .fileName(f.getOriginalFilename()).success(true).data(r).build();
                    } catch (Exception ex) {
                        return UploadItemResult.<ProgressPhotoResponse>builder()
                                .fileName(f.getOriginalFilename()).success(false).error(ex.getMessage()).build();
                    }
                })
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Progress photos processed", results));
    }

    /** GET /api/trainees/me/progress-photos – trainee lists their own progress photos */
    @GetMapping("/me/progress-photos")
    public ResponseEntity<ApiResponse<List<ProgressPhotoResponse>>> getMyProgressPhotos(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(progressPhotoService.getMyPhotos(principal.getUsername())));
    }

    /**
     * POST /api/trainees/me/progress-pictures – multipart compatibility alias.
     * Older clients use /progress-pictures while uploading files.
     */
    @PostMapping(value = "/me/progress-pictures", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<UploadItemResult<ProgressPhotoResponse>>>> uploadMyProgressPictureCompat(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "picture", required = false) MultipartFile picture,
            @RequestParam(value = "label", required = false) String label,
            @RequestParam(value = "photoDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate photoDate) {
        List<MultipartFile> toProcess = resolveFiles(files, file, photo, image, picture);
        List<UploadItemResult<ProgressPhotoResponse>> results = toProcess.stream()
                .map(f -> {
                    try {
                        ProgressPhotoResponse r = progressPhotoService.uploadPhotoByTrainee(
                                principal.getUsername(), f, label, photoDate);
                        return UploadItemResult.<ProgressPhotoResponse>builder()
                                .fileName(f.getOriginalFilename()).success(true).data(r).build();
                    } catch (Exception ex) {
                        return UploadItemResult.<ProgressPhotoResponse>builder()
                                .fileName(f.getOriginalFilename()).success(false).error(ex.getMessage()).build();
                    }
                })
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Progress photos processed", results));
    }

    /** DELETE /api/trainees/me/progress-photos/{id} – trainee deletes their own progress photo */
    @DeleteMapping("/me/progress-photos/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMyProgressPhoto(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        progressPhotoService.deletePhotoByTrainee(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Progress photo deleted", null));
    }

    /** POST /api/trainees/me/inbody-reports – trainee uploads one or more InBody reports */
    @PostMapping(value = "/me/inbody-reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<UploadItemResult<InBodyReportResponse>>>> uploadMyInBodyReport(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "picture", required = false) MultipartFile picture,
            @RequestParam(value = "label", required = false) String label,
            @RequestParam(value = "reportDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {
        List<MultipartFile> toProcess = resolveFiles(files, file, photo, image, picture);
        List<UploadItemResult<InBodyReportResponse>> results = toProcess.stream()
                .map(f -> {
                    try {
                        InBodyReportResponse r = inBodyReportService.uploadReportByTrainee(
                                principal.getUsername(), f, label, reportDate);
                        return UploadItemResult.<InBodyReportResponse>builder()
                                .fileName(f.getOriginalFilename()).success(true).data(r).build();
                    } catch (Exception ex) {
                        return UploadItemResult.<InBodyReportResponse>builder()
                                .fileName(f.getOriginalFilename()).success(false).error(ex.getMessage()).build();
                    }
                })
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("InBody reports processed", results));
    }

    /** GET /api/trainees/me/inbody-reports – trainee lists their own InBody reports */
    @GetMapping("/me/inbody-reports")
    public ResponseEntity<ApiResponse<List<InBodyReportResponse>>> getMyInBodyReports(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(inBodyReportService.getMyReports(principal.getUsername())));
    }

    /** GET /api/trainees/me/inbody-report – compatibility alias for singular legacy path */
    @GetMapping("/me/inbody-report")
    public ResponseEntity<ApiResponse<List<InBodyReportResponse>>> getMyInBodyReportsCompat(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(inBodyReportService.getMyReports(principal.getUsername())));
    }

    /** DELETE /api/trainees/me/inbody-reports/{id} – trainee deletes their own InBody report */
    @DeleteMapping("/me/inbody-reports/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMyInBodyReport(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        inBodyReportService.deleteReportByTrainee(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("InBody report deleted", null));
    }

    private List<MultipartFile> resolveFiles(List<MultipartFile> files, MultipartFile... singles) {
        if (files != null && !files.isEmpty()) {
            return files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        }
        for (MultipartFile candidate : singles) {
            if (candidate != null && !candidate.isEmpty()) {
                return List.of(candidate);
            }
        }
        throw new IllegalArgumentException("Missing file upload part. Use 'files' (multi) or one of: file, photo, image, picture.");
    }
}
