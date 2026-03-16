package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.ExercisePlan;
import com.fitcoach.domain.entity.Meal;
import com.fitcoach.domain.entity.NutritionPlan;
import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.domain.entity.TraineeMealCompletion;
import com.fitcoach.domain.entity.TraineeWorkoutCompletion;
import com.fitcoach.domain.entity.Workout;
import com.fitcoach.domain.entity.WorkoutExercise;
import com.fitcoach.dto.request.UpdateTraineeRequest;
import com.fitcoach.dto.response.CoachProfileResponse;
import com.fitcoach.dto.response.TraineeDashboardTodayResponse;
import com.fitcoach.dto.response.TraineeExercisePlanDetailResponse;
import com.fitcoach.dto.response.TraineeProfileResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.ExercisePlanRepository;
import com.fitcoach.repository.NutritionPlanRepository;
import com.fitcoach.repository.TraineeMealCompletionRepository;
import com.fitcoach.repository.TraineeRepository;
import com.fitcoach.repository.TraineeWorkoutCompletionRepository;
import com.fitcoach.repository.UserRepository;
import com.fitcoach.repository.WorkoutRepository;
import com.fitcoach.repository.MealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TraineeService {

    private final TraineeRepository traineeRepository;
    private final UserRepository userRepository;
    private final ExercisePlanRepository exercisePlanRepository;
    private final NutritionPlanRepository nutritionPlanRepository;
    private final TraineeWorkoutCompletionRepository traineeWorkoutCompletionRepository;
    private final TraineeMealCompletionRepository traineeMealCompletionRepository;
    private final WorkoutRepository workoutRepository;
    private final MealRepository mealRepository;

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

        var streak = TraineeDashboardTodayResponse.Streak.builder()
                .currentDays(0)
                .nextBadgeInDays(0)
                .nextBadgeName(null)
                .build();

        var coachGoals = java.util.List.<TraineeDashboardTodayResponse.CoachGoal>of();

        java.util.List<ExercisePlan> exercisePlans =
                exercisePlanRepository.findByTraineesId(trainee.getId());
        ExercisePlan todayExercisePlan = exercisePlans.stream()
                .max(java.util.Comparator.comparing(
                        ExercisePlan::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .orElse(null);

        TraineeDashboardTodayResponse.TodayWorkoutSummary workoutSummary = null;
        if (todayExercisePlan != null) {
            int totalExercises = todayExercisePlan.getWorkouts().stream()
                    .mapToInt(w -> w.getWorkoutExercises().size())
                    .sum();

            var completionsForPlanToday =
                    traineeWorkoutCompletionRepository.findByTraineeIdAndWorkoutExercisePlanIdAndCompletionDate(
                            trainee.getId(), todayExercisePlan.getId(), today);

            int exercisesDone = completionsForPlanToday.stream()
                    .map(TraineeWorkoutCompletion::getWorkout)
                    .mapToInt(w -> w.getWorkoutExercises().size())
                    .sum();

            int durationMinutes = todayExercisePlan.getWorkouts().size() * 10;
            int estimatedCalories = todayExercisePlan.getWorkouts().size() * 80;

            workoutSummary = TraineeDashboardTodayResponse.TodayWorkoutSummary.builder()
                    .planId(todayExercisePlan.getId())
                    .title(todayExercisePlan.getTitle())
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

            int caloriesConsumed = mealCompletionsToday.stream()
                    .map(TraineeMealCompletion::getMeal)
                    .map(Meal::getCalories)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Double::intValue)
                    .sum();

            nutritionSummary = TraineeDashboardTodayResponse.TodayNutritionSummary.builder()
                    .planId(todayNutritionPlan.getId())
                    .title(todayNutritionPlan.getTitle())
                    .caloriesConsumed(caloriesConsumed)
                    .caloriesTarget(0)
                    .proteinGrams(0)
                    .proteinTarget(0)
                    .carbsGrams(0)
                    .carbsTarget(0)
                    .fatGrams(0)
                    .fatTarget(0)
                    .build();
        }

        var weeklyGoals = TraineeDashboardTodayResponse.WeeklyGoals.builder()
                .workoutsCompleted(0)
                .workoutsTarget(0)
                .mealsLogged(0)
                .mealsTarget(0)
                .waterLiters(0.0)
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
    public TraineeExercisePlanDetailResponse getExercisePlanDetail(String email, Long planId) {
        Trainee trainee = getTraineeByEmail(email);

        ExercisePlan plan = exercisePlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise plan not found"));

        if (plan.getTrainees().stream().noneMatch(t -> t.getId().equals(trainee.getId()))) {
            throw new ResourceNotFoundException("Exercise plan not assigned to this trainee");
        }

        int exercisesTotal = plan.getWorkouts().stream()
                .mapToInt(w -> w.getWorkoutExercises().size())
                .sum();

        int durationMinutes = plan.getWorkouts().size() * 10;
        int estimatedCalories = plan.getWorkouts().size() * 80;

        int setsTotal = plan.getWorkouts().stream()
                .flatMap(w -> w.getWorkoutExercises().stream())
                .mapToInt(WorkoutExercise::getSets)
                .sum();

        var itemsBuilder = new java.util.ArrayList<TraineeExercisePlanDetailResponse.ExerciseItem>();
        plan.getWorkouts().forEach(workout -> workout.getWorkoutExercises().stream()
                .sorted(java.util.Comparator.comparingInt(WorkoutExercise::getOrderIndex))
                .forEach(we -> itemsBuilder.add(TraineeExercisePlanDetailResponse.ExerciseItem.builder()
                        .order(we.getOrderIndex())
                        .name(we.getExercise().getName())
                        .sets(we.getSets())
                        .reps(we.getReps())
                        .load(we.getLoad())
                        .rest(we.getRest())
                        .muscleGroup(we.getExercise().getTargetedMuscle())
                        .status("not_started")
                        .build())));

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
                .exercises(itemsBuilder)
                .build();
    }

    @Transactional
    public void completeWorkout(String email, Long workoutId) {
        Trainee trainee = getTraineeByEmail(email);
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout not found"));

        var today = java.time.LocalDate.now();
        traineeWorkoutCompletionRepository
                .findByTraineeIdAndWorkoutIdAndCompletionDate(trainee.getId(), workoutId, today)
                .ifPresentOrElse(
                        existing -> {},
                        () -> traineeWorkoutCompletionRepository.save(
                                TraineeWorkoutCompletion.builder()
                                        .trainee(trainee)
                                        .workout(workout)
                                        .completionDate(today)
                                        .completedAt(java.time.LocalDateTime.now())
                                        .build()));
    }

    @Transactional
    public void completeMeal(String email, Long mealId) {
        Trainee trainee = getTraineeByEmail(email);
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal not found"));

        var today = java.time.LocalDate.now();
        traineeMealCompletionRepository
                .findByTraineeIdAndMealIdAndCompletionDate(trainee.getId(), mealId, today)
                .ifPresentOrElse(
                        existing -> {},
                        () -> traineeMealCompletionRepository.save(
                                TraineeMealCompletion.builder()
                                        .trainee(trainee)
                                        .meal(meal)
                                        .completionDate(today)
                                        .completedAt(java.time.LocalDateTime.now())
                                        .build()));
    }

    @Transactional
    public TraineeProfileResponse updateMyProfile(String email, UpdateTraineeRequest request) {
        Trainee trainee = getTraineeByEmail(email);

        if (StringUtils.hasText(request.getFullName())) {
            trainee.getUser().setFullName(request.getFullName());
        }
        if (StringUtils.hasText(request.getFitnessGoal())) {
            trainee.setFitnessGoal(request.getFitnessGoal());
        }

        traineeRepository.save(trainee);
        return toResponse(trainee);
    }

    @Transactional(readOnly = true)
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

    // ── Helpers ───────────────────────────────────────────────────────────────
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
