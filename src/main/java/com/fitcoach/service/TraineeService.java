package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.ExtraMealLog;
import com.fitcoach.domain.entity.Ingredient;
import com.fitcoach.domain.entity.Meal;
import com.fitcoach.domain.entity.NutritionPlan;
import com.fitcoach.domain.entity.PlanSession;
import com.fitcoach.domain.entity.PlanSessionExercise;
import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.domain.entity.TraineeMealCompletion;
import com.fitcoach.domain.entity.TraineeWorkoutCompletion;
import com.fitcoach.domain.entity.WorkoutPlan;
import com.fitcoach.domain.entity.CoachGoal;
import com.fitcoach.domain.enums.GoalStatus;
import com.fitcoach.dto.request.ExtraMealRequest;
import com.fitcoach.dto.request.IngredientSwapRequest;
import com.fitcoach.dto.request.MealCompletionRequest;
import com.fitcoach.dto.request.UpdateTraineeRequest;
import com.fitcoach.dto.response.ExtraMealLogResponse;
import com.fitcoach.dto.response.CoachProfileResponse;
import com.fitcoach.dto.response.TraineeDashboardTodayResponse;
import com.fitcoach.dto.response.TraineeExercisePlanDetailResponse;
import com.fitcoach.dto.response.TraineeProfileResponse;
import com.fitcoach.dto.response.TraineeWorkoutPlanSessionsResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.domain.entity.MealIngredientDeviation;
import com.fitcoach.repository.CoachGoalRepository;
import com.fitcoach.repository.ExtraMealLogRepository;
import com.fitcoach.repository.IngredientRepository;
import com.fitcoach.repository.MealIngredientDeviationRepository;
import com.fitcoach.repository.MealRepository;
import com.fitcoach.repository.NutritionPlanRepository;
import com.fitcoach.repository.PlanAssignmentRepository;
import com.fitcoach.repository.PlanSessionRepository;
import com.fitcoach.repository.TraineeMealCompletionRepository;
import com.fitcoach.repository.TraineeRepository;
import com.fitcoach.repository.TraineeWaterIntakeRepository;
import com.fitcoach.repository.TraineeWorkoutCompletionRepository;
import com.fitcoach.repository.UserRepository;
import com.fitcoach.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TraineeService {

    private final TraineeRepository traineeRepository;
    private final UserRepository userRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final PlanAssignmentRepository planAssignmentRepository;
    private final PlanSessionRepository planSessionRepository;
    private final ExercisePlanService exercisePlanService;
    private final NutritionPlanRepository nutritionPlanRepository;
    private final TraineeWorkoutCompletionRepository traineeWorkoutCompletionRepository;
    private final TraineeMealCompletionRepository traineeMealCompletionRepository;
    private final MealRepository mealRepository;
    private final ExtraMealLogRepository extraMealLogRepository;
    private final IngredientRepository ingredientRepository;
    private final MealIngredientDeviationRepository mealIngredientDeviationRepository;
    private final CoachGoalRepository coachGoalRepository;
    private final TraineeWaterIntakeRepository traineeWaterIntakeRepository;

    private static final int[] STREAK_BADGE_THRESHOLDS = {7, 14, 30, 60, 100, 180, 365};
    private static final String[] STREAK_BADGE_NAMES = {
        "Week Warrior", "Fortnight Fighter", "Monthly Master",
        "2-Month Champion", "Century Club", "Half-Year Hero", "Year Legend"
    };

    @Transactional(readOnly = true)
    public TraineeProfileResponse getMyProfile(String email) {
        return toResponse(getTraineeByEmail(email));
    }

    @Transactional(readOnly = true)
    public TraineeDashboardTodayResponse getTodayDashboard(String email) {
        Trainee trainee = getTraineeByEmail(email);
        Coach coach = trainee.getCoach();
        var today = java.time.LocalDate.now();

        var profile = TraineeDashboardTodayResponse.Profile.builder()
                .id(trainee.getId())
                .fullName(trainee.getUser().getFullName())
                .fitnessGoal(trainee.getFitnessGoal())
                .avatarUrl(null)
                .build();

        var coachSummary = TraineeDashboardTodayResponse.CoachSummary.builder()
                .id(coach.getId())
                .fullName(coach.getUser().getFullName())
                .build();

        int currentStreak = computeStreak(trainee.getId());
        int[] nextBadge = nextStreakBadge(currentStreak);
        var streak = TraineeDashboardTodayResponse.Streak.builder()
                .currentDays(currentStreak)
                .nextBadgeInDays(nextBadge[0])
                .nextBadgeName(STREAK_BADGE_NAMES[nextBadge[1]])
                .build();

        var coachGoals = coachGoalRepository
                .findByTraineeIdOrderByCreatedAtDesc(trainee.getId())
                .stream()
                .map(g -> TraineeDashboardTodayResponse.CoachGoal.builder()
                        .id(g.getId())
                        .label(g.getTitle())
                        .completed(g.getStatus() == GoalStatus.COMPLETED)
                        .build())
                .collect(java.util.stream.Collectors.toList());

        java.util.List<WorkoutPlan> workoutPlans = exercisePlanService.getPlansByTrainee(trainee.getId());
        WorkoutPlan todayWorkoutPlan = workoutPlans.stream()
                .max(java.util.Comparator.comparing(
                        WorkoutPlan::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .orElse(null);

        TraineeDashboardTodayResponse.TodayWorkoutSummary workoutSummary = null;
        if (todayWorkoutPlan != null) {
            int totalExercises = todayWorkoutPlan.getSessions().stream()
                    .mapToInt(s -> s.getSessionExercises().size())
                    .sum();

            var completionsForPlanToday =
                    traineeWorkoutCompletionRepository.findByTrainee_IdAndPlanSession_Plan_IdAndCompletionDate(
                            trainee.getId(), todayWorkoutPlan.getId(), today);

            int exercisesDone = completionsForPlanToday.stream()
                    .map(TraineeWorkoutCompletion::getPlanSession)
                    .mapToInt(s -> s.getSessionExercises().size())
                    .sum();

            int durationMinutes = todayWorkoutPlan.getSessions().size() * 10;
            int estimatedCalories = todayWorkoutPlan.getSessions().size() * 80;

            workoutSummary = TraineeDashboardTodayResponse.TodayWorkoutSummary.builder()
                    .planId(todayWorkoutPlan.getId())
                    .title(todayWorkoutPlan.getTitle())
                    .difficulty("medium")
                    .exercisesTotal(totalExercises)
                    .exercisesDone(exercisesDone)
                    .durationMinutes(durationMinutes)
                    .estimatedCalories(estimatedCalories)
                    .build();
        }

        java.util.List<NutritionPlan> nutritionPlans =
                nutritionPlanRepository.findByTraineesId(trainee.getId());
        NutritionPlan todayNutritionPlan = nutritionPlans.stream()
                .max(java.util.Comparator.comparing(
                        NutritionPlan::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .orElse(null);

        TraineeDashboardTodayResponse.TodayNutritionSummary nutritionSummary = null;
        if (todayNutritionPlan != null) {
            var mealCompletionsToday =
                    traineeMealCompletionRepository.findByTraineeIdAndMealNutritionPlanIdAndCompletionDate(
                            trainee.getId(), todayNutritionPlan.getId(), today);

            java.util.Set<Long> completedMealIds = mealCompletionsToday.stream()
                    .map(c -> c.getMeal().getId())
                    .collect(java.util.stream.Collectors.toSet());

            int caloriesTarget = 0, proteinTarget = 0, carbsTarget = 0, fatTarget = 0;
            int caloriesConsumed = 0, proteinGrams = 0, carbsGrams = 0, fatGrams = 0;

            var dashboardMeals = new java.util.ArrayList<TraineeDashboardTodayResponse.DashboardMeal>();

            for (Meal meal : todayNutritionPlan.getMeals()) {
                boolean completed = completedMealIds.contains(meal.getId());
                int cal = meal.getCalories() != null ? meal.getCalories().intValue() : 0;
                int pro = meal.getProtein() != null ? meal.getProtein().intValue() : 0;
                int car = meal.getCarbs() != null ? meal.getCarbs().intValue() : 0;
                int fat = meal.getFat() != null ? meal.getFat().intValue() : 0;

                caloriesTarget += cal;
                proteinTarget += pro;
                carbsTarget += car;
                fatTarget += fat;

                if (completed) {
                    caloriesConsumed += cal;
                    proteinGrams += pro;
                    carbsGrams += car;
                    fatGrams += fat;
                }

                dashboardMeals.add(TraineeDashboardTodayResponse.DashboardMeal.builder()
                        .id(meal.getId())
                        .name(meal.getName())
                        .calories(cal)
                        .completed(completed)
                        .build());
            }

            nutritionSummary = TraineeDashboardTodayResponse.TodayNutritionSummary.builder()
                    .planId(todayNutritionPlan.getId())
                    .title(todayNutritionPlan.getTitle())
                    .caloriesConsumed(caloriesConsumed)
                    .caloriesTarget(caloriesTarget)
                    .proteinGrams(proteinGrams)
                    .proteinTarget(proteinTarget)
                    .carbsGrams(carbsGrams)
                    .carbsTarget(carbsTarget)
                    .fatGrams(fatGrams)
                    .fatTarget(fatTarget)
                    .meals(dashboardMeals)
                    .build();
        }

        double todayWaterLiters = traineeWaterIntakeRepository
                .findByTraineeIdAndIntakeDate(trainee.getId(), today)
                .map(w -> w.getLiters() != null ? w.getLiters() : 0.0)
                .orElse(0.0);

        var weeklyGoals = TraineeDashboardTodayResponse.WeeklyGoals.builder()
                .workoutsCompleted(0)
                .workoutsTarget(0)
                .mealsLogged(0)
                .mealsTarget(0)
                .waterLiters(todayWaterLiters)
                .waterTargetLiters(0.0)
                .weightStart(0.0)
                .weightCurrent(0.0)
                .weightTarget(0.0)
                .build();

        var achievements = java.util.List.<TraineeDashboardTodayResponse.Achievement>of();

        return TraineeDashboardTodayResponse.builder()
                .profile(profile)
                .coach(coachSummary)
                .streak(streak)
                .coachGoals(coachGoals)
                .todayWorkoutSummary(workoutSummary)
                .todayNutritionSummary(nutritionSummary)
                .weeklyGoals(weeklyGoals)
                .achievements(achievements)
                .build();
    }

    @Transactional(readOnly = true)
    public TraineeExercisePlanDetailResponse getExercisePlanDetail(String email, UUID planId) {
        Trainee trainee = getTraineeByEmail(email);

        if (!planAssignmentRepository.existsByPlan_IdAndTrainee_Id(planId, trainee.getId())) {
            throw new ResourceNotFoundException("Exercise plan not assigned to this trainee");
        }

        WorkoutPlan plan = workoutPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise plan not found"));

        int exercisesTotal = plan.getSessions().stream()
                .mapToInt(s -> s.getSessionExercises().size())
                .sum();

        int durationMinutes = plan.getSessions().size() * 10;
        int estimatedCalories = plan.getSessions().size() * 80;

        int setsTotal = plan.getSessions().stream()
                .flatMap(s -> s.getSessionExercises().stream())
                .mapToInt(PlanSessionExercise::getSets)
                .sum();

        var sortedSessions = plan.getSessions().stream()
                .sorted(java.util.Comparator.comparing(
                        PlanSession::getDayOrder,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();

        var sessionSummaries = new java.util.ArrayList<TraineeExercisePlanDetailResponse.SessionSummary>();
        var itemsBuilder = new java.util.ArrayList<TraineeExercisePlanDetailResponse.ExerciseItem>();

        for (PlanSession session : sortedSessions) {
            var lines = session.getSessionExercises().stream()
                    .sorted(java.util.Comparator.comparingInt(PlanSessionExercise::getOrderIndex))
                    .toList();

            sessionSummaries.add(TraineeExercisePlanDetailResponse.SessionSummary.builder()
                    .sessionId(session.getId())
                    .title(session.getTitle())
                    .dayOrder(session.getDayOrder())
                    .exerciseCount(lines.size())
                    .build());

            for (PlanSessionExercise we : lines) {
                itemsBuilder.add(TraineeExercisePlanDetailResponse.ExerciseItem.builder()
                        .id(we.getId())
                        .sessionId(session.getId())
                        .sessionTitle(session.getTitle())
                        .dayOrder(session.getDayOrder())
                        .order(we.getOrderIndex())
                        .name(we.getExercise().getName())
                        .sets(we.getSets())
                        .reps(we.getReps())
                        .load(we.getLoadAmount() != null ? we.getLoadAmount().toPlainString() : null)
                        .rest(we.getRestSeconds() != null ? we.getRestSeconds() + "s" : null)
                        .muscleGroup(we.getExercise().getTargetedMuscle())
                        .status("not_started")
                        .build());
            }
        }

        return TraineeExercisePlanDetailResponse.builder()
                .id(plan.getId())
                .title(plan.getTitle())
                .subtitle(plan.getDescription())
                .difficulty("medium")
                .exercisesTotal(exercisesTotal)
                .durationMinutes(durationMinutes)
                .estimatedCalories(estimatedCalories)
                .setsTotal(setsTotal)
                .coachNote(plan.getDescription())
                .sessions(sessionSummaries)
                .exercises(itemsBuilder)
                .build();
    }

    /**
     * Assigned plan only: ordered plan sessions (titles / day order / counts) with no exercise payload.
     */
    @Transactional(readOnly = true)
    public TraineeWorkoutPlanSessionsResponse getWorkoutPlanSessions(String email, UUID planId) {
        Trainee trainee = getTraineeByEmail(email);

        if (!planAssignmentRepository.existsByPlan_IdAndTrainee_Id(planId, trainee.getId())) {
            throw new ResourceNotFoundException("Exercise plan not assigned to this trainee");
        }

        WorkoutPlan plan = workoutPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise plan not found"));

        var rows = plan.getSessions().stream()
                .sorted(java.util.Comparator.comparing(
                        PlanSession::getDayOrder,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(session -> TraineeWorkoutPlanSessionsResponse.PlanSessionRow.builder()
                        .sessionId(session.getId())
                        .title(session.getTitle())
                        .dayOrder(session.getDayOrder())
                        .exerciseCount(session.getSessionExercises().size())
                        .build())
                .toList();

        return TraineeWorkoutPlanSessionsResponse.builder()
                .planId(plan.getId())
                .planTitle(plan.getTitle())
                .planDescription(plan.getDescription())
                .sessions(rows)
                .build();
    }

    @Transactional
    public void completePlanSession(String email, UUID planSessionId) {
        Trainee trainee = getTraineeByEmail(email);
        var session = planSessionRepository.findById(planSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan session not found"));

        if (!planAssignmentRepository.existsByPlan_IdAndTrainee_Id(session.getPlan().getId(), trainee.getId())) {
            throw new ResourceNotFoundException("Plan session not part of your assigned plans");
        }

        var today = java.time.LocalDate.now();
        traineeWorkoutCompletionRepository
                .findByTrainee_IdAndPlanSession_IdAndCompletionDate(trainee.getId(), planSessionId, today)
                .ifPresentOrElse(
                        existing -> {},
                        () -> traineeWorkoutCompletionRepository.save(
                                TraineeWorkoutCompletion.builder()
                                        .trainee(trainee)
                                        .planSession(session)
                                        .completionDate(today)
                                        .completedAt(java.time.LocalDateTime.now())
                                        .build()));
    }

@Transactional
    public void completeMeal(String email, Long mealId, MealCompletionRequest request) {
        Trainee trainee = getTraineeByEmail(email);
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal not found"));

        var today = java.time.LocalDate.now();
        
        // 1. Fetch existing log for today, or create a new one
        TraineeMealCompletion completion = traineeMealCompletionRepository
                .findByTraineeIdAndMealIdAndCompletionDate(trainee.getId(), mealId, today)
                .orElseGet(() -> TraineeMealCompletion.builder()
                        .trainee(trainee)
                        .meal(meal)
                        .completionDate(today)
                        .build());

        // 2. Update completion status
        completion.setCompletedAt(java.time.LocalDateTime.now());
        
        // Set whether the whole meal was skipped
        boolean isSkipped = request != null && request.isSkipMeal();
        completion.setSkipped(isSkipped); 

        // Save the main completion record
        traineeMealCompletionRepository.save(completion);

        // 3. Handle specific ingredient deviations (only if the meal was actually eaten)
        if (!isSkipped && request != null) {
            processIngredientDeviations(completion, request);
        }
    }

    private void processIngredientDeviations(TraineeMealCompletion completion, MealCompletionRequest request) {
        // Handle skipped ingredients – replacementIngredient stays null
        if (request.getSkippedIngredientIds() != null && !request.getSkippedIngredientIds().isEmpty()) {
            for (Long ingredientId : request.getSkippedIngredientIds()) {
                Ingredient original = ingredientRepository.findById(ingredientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found: " + ingredientId));
                mealIngredientDeviationRepository.save(MealIngredientDeviation.builder()
                        .completion(completion)
                        .originalIngredient(original)
                        .replacementIngredient(null)
                        .newQuantity(null)
                        .build());
            }
        }

        // Handle replaced ingredients – both original and replacement are stored
        if (request.getReplacedIngredients() != null && !request.getReplacedIngredients().isEmpty()) {
            for (IngredientSwapRequest swap : request.getReplacedIngredients()) {
                Ingredient original = ingredientRepository.findById(swap.getOriginalIngredientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found: " + swap.getOriginalIngredientId()));
                Ingredient replacement = ingredientRepository.findById(swap.getNewIngredientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found: " + swap.getNewIngredientId()));
                mealIngredientDeviationRepository.save(MealIngredientDeviation.builder()
                        .completion(completion)
                        .originalIngredient(original)
                        .replacementIngredient(replacement)
                        .newQuantity(swap.getNewQuantity())
                        .build());
            }
        }
    }

    @Transactional
    public TraineeProfileResponse updateMyProfile(String email, UpdateTraineeRequest request) {
        // ... existing logic remains exactly the same ...
        Trainee trainee = getTraineeByEmail(email);

        if (StringUtils.hasText(request.getFullName())) {
            trainee.getUser().setFullName(request.getFullName());
        }
        if (StringUtils.hasText(request.getFitnessGoal())) {
            trainee.setFitnessGoal(request.getFitnessGoal());
        }

        traineeRepository.save(trainee);
        return toResponse(trainee);
    }    @Transactional(readOnly = true)
    public CoachProfileResponse getMyCoach(String email) {
        Trainee trainee = getTraineeByEmail(email);
        var coach = trainee.getCoach();
        return CoachProfileResponse.builder()
                .id(coach.getId())
                .userId(coach.getUser().getId())
                .fullName(coach.getUser().getFullName())
                .email(coach.getUser().getEmail())
                .specialisation(coach.getSpecialisation())
                .bio(coach.getBio())
                .traineeCount(coach.getTrainees().size())
                .createdAt(coach.getCreatedAt())
                .build();
    }

    @Transactional
    public ExtraMealLogResponse logExtraMeal(String email, ExtraMealRequest request) {
        Trainee trainee = getTraineeByEmail(email);

        if (request.getIngredientId() == null && (request.getName() == null || request.getName().isBlank())) {
            throw new IllegalArgumentException("Either ingredientId or a free-text name is required");
        }

        Ingredient ingredient = null;
        String name;
        if (request.getIngredientId() != null) {
            ingredient = ingredientRepository.findById(request.getIngredientId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ingredient not found: " + request.getIngredientId()));
            name = ingredient.getName();
        } else {
            name = request.getName().trim();
        }

        ExtraMealLog log = ExtraMealLog.builder()
                .trainee(trainee)
                .name(name)
                .ingredient(ingredient)
                .calories(request.getCalories())
                .mealDate(request.getDate() != null ? request.getDate() : java.time.LocalDate.now())
                .build();

        return toExtraMealResponse(extraMealLogRepository.save(log));
    }

    private ExtraMealLogResponse toExtraMealResponse(ExtraMealLog log) {
        return ExtraMealLogResponse.builder()
                .id(log.getId())
                .traineeId(log.getTrainee().getId())
                .ingredientId(log.getIngredient() != null ? log.getIngredient().getId() : null)
                .name(log.getName())
                .calories(log.getCalories())
                .mealDate(log.getMealDate())
                .loggedAt(log.getLoggedAt())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Count consecutive days ending today where the trainee completed at least one workout. */
    public int computeStreak(Long traineeId) {
        java.util.Set<java.time.LocalDate> completionDates =
                traineeWorkoutCompletionRepository
                        .findByTrainee_IdOrderByCompletionDateDescCompletedAtDesc(traineeId)
                        .stream()
                        .map(TraineeWorkoutCompletion::getCompletionDate)
                        .collect(java.util.stream.Collectors.toSet());

        int streak = 0;
        java.time.LocalDate date = java.time.LocalDate.now();
        while (completionDates.contains(date)) {
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }

    /** Returns [daysToNextBadge, badgeNameIndex]. */
    private int[] nextStreakBadge(int currentStreak) {
        for (int i = 0; i < STREAK_BADGE_THRESHOLDS.length; i++) {
            if (currentStreak < STREAK_BADGE_THRESHOLDS[i]) {
                return new int[]{STREAK_BADGE_THRESHOLDS[i] - currentStreak, i};
            }
        }
        return new int[]{0, STREAK_BADGE_NAMES.length - 1};
    }

    public Trainee getTraineeByEmail(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return traineeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainee profile not found"));
    }

    private TraineeProfileResponse toResponse(Trainee trainee) {
        var coach = trainee.getCoach();
        return TraineeProfileResponse.builder()
                .id(trainee.getId())
                .userId(trainee.getUser().getId())
                .fullName(trainee.getUser().getFullName())
                .email(trainee.getUser().getEmail())
                .fitnessGoal(trainee.getFitnessGoal())
                .coachId(coach.getId())
                .coachName(coach.getUser().getFullName())
                .createdAt(trainee.getCreatedAt())
                .build();
    }
}
