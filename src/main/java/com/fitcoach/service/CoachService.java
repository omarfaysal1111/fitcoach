package com.fitcoach.service;

import com.fitcoach.domain.entity.MealIngredientDeviation;
import com.fitcoach.domain.entity.TraineeMealCompletion;
import com.fitcoach.dto.response.MealCompletionLogResponse;
import com.fitcoach.repository.MealIngredientDeviationRepository;
import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.NutritionPlan;
import com.fitcoach.domain.entity.PlanAssignment;
import com.fitcoach.domain.entity.PlanSession;
import com.fitcoach.domain.entity.PlanStatus;
import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.domain.enums.TraineeStatus;
import com.fitcoach.dto.request.UpdateCoachRequest;
import com.fitcoach.dto.request.UpdateTraineeByCoachRequest;
import com.fitcoach.dto.request.UpdateTraineeNotesRequest;
import com.fitcoach.dto.response.CoachGoalResponse;
import com.fitcoach.dto.response.InBodyReportResponse;
import com.fitcoach.dto.response.ProgressPhotoResponse;
import com.fitcoach.dto.response.CoachAlertResponse;
import com.fitcoach.dto.response.CoachTraineeDetailResponse;
import com.fitcoach.dto.response.CoachHomeResponse;
import com.fitcoach.dto.response.CoachProfileResponse;
import com.fitcoach.dto.response.InvitationResponse;
import com.fitcoach.dto.response.TraineeProfileResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.PlanAssignmentRepository;
import com.fitcoach.repository.NutritionPlanRepository;
import com.fitcoach.repository.TraineeMealCompletionRepository;
import com.fitcoach.repository.TraineeRepository;
import com.fitcoach.repository.TraineeWorkoutCompletionRepository;
import com.fitcoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachService {

    private final CoachRepository coachRepository;
    private final TraineeRepository traineeRepository;
    private final UserRepository userRepository;
    private final InvitationService invitationService;
    private final PlanAssignmentRepository planAssignmentRepository;
    private final NutritionPlanRepository nutritionPlanRepository;
    private final TraineeWorkoutCompletionRepository traineeWorkoutCompletionRepository;
    private final TraineeMealCompletionRepository traineeMealCompletionRepository;
    private final MealIngredientDeviationRepository mealIngredientDeviationRepository;
    private final ExerciseLogService exerciseLogService;
    private final MeasurementService measurementService;
    private final CoachGoalService coachGoalService;
    private final InBodyReportService inBodyReportService;
    private final ProgressPhotoService progressPhotoService;
    private final TraineeService traineeService;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public CoachProfileResponse getMyProfile(String email) {
        Coach coach = findCoachByEmail(email);
        return toResponse(coach);
    }

    /**
     * Aggregated view used by the Coach Home screen in the mobile app.
     * Combines the coach profile, all trainees, and all invitations
     * into a single payload.
     */
    @Transactional(readOnly = true)
    public CoachHomeResponse getHome(String email) {
        Coach coach = findCoachByEmail(email);

        List<TraineeProfileResponse> trainees = traineeRepository.findAllByCoachId(coach.getId())
                .stream()
                .map(t -> toTraineeResponse(t, coach))
                .collect(Collectors.toList());

        List<InvitationResponse> invitations = invitationService.getInvitationsForCoach(email);

        return CoachHomeResponse.builder()
                .coach(toResponse(coach))
                .trainees(trainees)
                .invitations(invitations)
                .build();
    }

    @Transactional
    public CoachProfileResponse updateMyProfile(String email, UpdateCoachRequest request) {
        Coach coach = findCoachByEmail(email);

        if (StringUtils.hasText(request.getFullName())) {
            coach.getUser().setFullName(request.getFullName());
        }
        if (request.getBio() != null) {
            coach.setBio(request.getBio());
        }
        if (StringUtils.hasText(request.getSpecialisation())) {
            coach.setSpecialisation(request.getSpecialisation());
        }

        coachRepository.save(coach);
        return toResponse(coach);
    }

    @Transactional
    public TraineeProfileResponse updateTraineeNotes(String coachEmail, Long traineeId,
            UpdateTraineeNotesRequest request) {
        Coach coach = findCoachByEmail(coachEmail);
        Trainee trainee = traineeRepository.findById(traineeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainee not found"));
        if (!trainee.getCoach().getId().equals(coach.getId())) {
            throw new IllegalArgumentException("Not authorized to update this trainee");
        }
        if (request.getCoachFeedback() != null) {
            trainee.setCoachFeedback(request.getCoachFeedback());
        }
        if (request.getCautionNotes() != null) {
            trainee.setCautionNotes(request.getCautionNotes());
        }
        traineeRepository.save(trainee);
        return toTraineeResponse(trainee, coach);
    }

    @Transactional
    public TraineeProfileResponse updateTraineeByCoach(String coachEmail, Long traineeId,
            UpdateTraineeByCoachRequest request) {
        Coach coach = findCoachByEmail(coachEmail);
        Trainee trainee = traineeRepository.findById(traineeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainee not found"));
        if (!trainee.getCoach().getId().equals(coach.getId())) {
            throw new IllegalArgumentException("Not authorized to update this trainee");
        }
        if (StringUtils.hasText(request.getFitnessGoal())) {
            trainee.setFitnessGoal(request.getFitnessGoal());
        }
        if (StringUtils.hasText(request.getTraineeLevel())) {
            trainee.setTraineeLevel(request.getTraineeLevel());
        }
        traineeRepository.save(trainee);
        return toTraineeResponse(trainee, coach);
    }

    @Transactional(readOnly = true)
    public List<TraineeProfileResponse> getMyTrainees(String email) {
        Coach coach = findCoachByEmail(email);
        return traineeRepository.findAllByCoachId(coach.getId())
                .stream()
                .map(t -> toTraineeResponse(t, coach))
                .collect(Collectors.toList());
    }

    // ── Alert threshold: only flag trainees below this combined adherence % ──────
    private static final int ALERT_THRESHOLD_PCT = 65;
    // ── Minimum completed days of plan coverage before we can meaningfully alert ─
    private static final long MIN_COVERAGE_DAYS = 3;

    @Transactional(readOnly = true)
    public List<CoachAlertResponse> getAlerts(String email) {
        Coach coach = findCoachByEmail(email);
        // Use yesterday as window end — today is incomplete so we only judge closed days.
        LocalDate yesterday = LocalDate.now().minusDays(1);

        record PendingAlert(Trainee trainee, Adherence7d adherence) {}
        List<PendingAlert> pending = new ArrayList<>();

        for (Trainee trainee : traineeRepository.findAllByCoachId(coach.getId())) {
            LocalDate cycleStart = cycleWindowStart(trainee);
            // If the cycle started today, yesterday is before the window → no completed data.
            if (cycleStart.isAfter(yesterday)) continue;

            Adherence7d a = computeAdherence7d(trainee.getId(), cycleStart, yesterday);

            // Skip trainees with no active plans or insufficient coverage to avoid false positives.
            if (!a.hasAssignablePlans() || a.effectiveCoverageDays < MIN_COVERAGE_DAYS) continue;

            // Only raise an alert when adherence is meaningfully below the threshold.
            if (a.combinedPercent() >= ALERT_THRESHOLD_PCT) continue;

            pending.add(new PendingAlert(trainee, a));
        }

        // Worst adherence first.
        pending.sort(Comparator.comparingInt(p -> p.adherence().combinedPercent()));

        List<CoachAlertResponse> alerts = new ArrayList<>();
        for (PendingAlert p : pending) {
            Adherence7d a = p.adherence();
            String alertType = resolveAlertType(a);
            alerts.add(CoachAlertResponse.builder()
                    .id("alert-" + p.trainee().getId())
                    .traineeId(String.valueOf(p.trainee().getId()))
                    .traineeName(p.trainee().getUser().getFullName())
                    .message(formatAlertMessage(a))
                    .type(alertType)
                    .adherencePct(a.combinedPercent())
                    .build());
        }
        return alerts;
    }

    /** Picks the dominant alert type based on which metric is worse. */
    private static String resolveAlertType(Adherence7d a) {
        boolean workoutLow = a.workoutPct >= 0 && a.workoutPct < ALERT_THRESHOLD_PCT;
        boolean nutritionLow = a.nutritionPct >= 0 && a.nutritionPct < ALERT_THRESHOLD_PCT;
        if (workoutLow && nutritionLow) {
            return a.workoutPct <= a.nutritionPct ? "missed" : "nutrition";
        }
        if (workoutLow) return "missed";
        if (nutritionLow) return "nutrition";
        // Both plans exist but combined average is below threshold — flag the worse one.
        if (a.workoutPct >= 0 && a.nutritionPct >= 0) {
            return a.workoutPct <= a.nutritionPct ? "missed" : "nutrition";
        }
        return a.workoutPct >= 0 ? "missed" : "nutrition";
    }

    /** Single human-readable line describing the dominant problem. */
    private static String formatAlertMessage(Adherence7d a) {
        String type = resolveAlertType(a);
        if ("missed".equals(type)) {
            int missed = (int) Math.ceil(Math.max(1, a.workoutExpected - a.workoutActual));
            return "Missed " + missed + "+ workout" + (missed == 1 ? "" : "s") + " this week";
        }
        return "Nutrition adherence dropped to " + a.nutritionPct + "%";
    }

    /**
     * Returns the start of the trainee's current 7-day adherence cycle.
     * Cycles are anchored to the trainee's join date so the window resets cleanly every 7 days.
     */
    private static LocalDate cycleWindowStart(Trainee trainee) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = trainee.getCreatedAt() != null
                ? trainee.getCreatedAt().toLocalDate()
                : today;
        long daysSinceStart = ChronoUnit.DAYS.between(startDate, today);
        long cycleNumber = daysSinceStart / 7;
        return startDate.plusDays(cycleNumber * 7);
    }

    /**
     * Computes adherence over [windowStart, windowEnd] (both inclusive, windowEnd = yesterday).
     * Only counts completed plan-session days so new trainees don't get flagged on day 1.
     */
    private Adherence7d computeAdherence7d(Long traineeId, LocalDate windowStart, LocalDate windowEnd) {
        double workoutExpected = 0;
        double workoutActual = 0;
        long effectiveCoverageDays = 0;
        Set<UUID> workoutSessionIds = new HashSet<>();
        List<PlanAssignment> activeAssignments =
                planAssignmentRepository.findByTrainee_IdAndStatus(traineeId, PlanStatus.ACTIVE);

        for (PlanAssignment assignment : activeAssignments) {
            LocalDate effectiveStart = assignment.getStartDate().isAfter(windowStart)
                    ? assignment.getStartDate()
                    : windowStart;
            if (effectiveStart.isAfter(windowEnd)) continue;

            long overlapDays = ChronoUnit.DAYS.between(effectiveStart, windowEnd) + 1;
            effectiveCoverageDays = Math.max(effectiveCoverageDays, overlapDays);

            List<PlanSession> sessions = assignment.getPlan().getSessions();
            int sessionCount = sessions.size();
            if (sessionCount == 0) continue;

            workoutExpected += sessionCount * (overlapDays / 7.0);
            for (PlanSession s : sessions) {
                if (s.getId() != null) workoutSessionIds.add(s.getId());
            }
        }

        if (!workoutSessionIds.isEmpty()) {
            Map<UUID, Long> completionsPerSession = new HashMap<>();
            traineeWorkoutCompletionRepository
                    .findByTrainee_IdAndCompletionDateBetween(traineeId, windowStart, windowEnd)
                    .stream()
                    .filter(c -> workoutSessionIds.contains(c.getPlanSession().getId()))
                    .forEach(c -> completionsPerSession.merge(c.getPlanSession().getId(), 1L, Long::sum));
            for (UUID sid : workoutSessionIds) {
                workoutActual += Math.min(1L, completionsPerSession.getOrDefault(sid, 0L));
            }
        }

        int workoutPct = workoutExpected <= 0
                ? -1
                : (int) Math.round(Math.min(100.0, 100.0 * workoutActual / workoutExpected));

        List<NutritionPlan> nutritionPlans = nutritionPlanRepository.findByTraineesId(traineeId);
        Set<Long> mealIds = nutritionPlans.stream()
                .flatMap(p -> p.getMeals().stream())
                .map(m -> m.getId())
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        long windowDays = ChronoUnit.DAYS.between(windowStart, windowEnd) + 1;
        double nutritionExpected = mealIds.isEmpty() ? 0 : (double) mealIds.size() * windowDays;
        long nutritionActual = mealIds.isEmpty()
                ? 0
                : traineeMealCompletionRepository
                        .findByTraineeIdAndCompletionDateBetween(traineeId, windowStart, windowEnd)
                        .stream()
                        .filter(c -> mealIds.contains(c.getMeal().getId()))
                        .count();

        int nutritionPct = nutritionExpected <= 0
                ? -1
                : (int) Math.round(Math.min(100.0, 100.0 * nutritionActual / nutritionExpected));

        return new Adherence7d(workoutPct, nutritionPct, workoutExpected, workoutActual, effectiveCoverageDays);
    }

    private static final class Adherence7d {
        final int workoutPct;
        final int nutritionPct;
        final double workoutExpected;
        final double workoutActual;
        final long effectiveCoverageDays;

        Adherence7d(int workoutPct, int nutritionPct,
                    double workoutExpected, double workoutActual, long effectiveCoverageDays) {
            this.workoutPct = workoutPct;
            this.nutritionPct = nutritionPct;
            this.workoutExpected = workoutExpected;
            this.workoutActual = workoutActual;
            this.effectiveCoverageDays = effectiveCoverageDays;
        }

        boolean hasAssignablePlans() {
            return workoutPct >= 0 || nutritionPct >= 0;
        }

        int combinedPercent() {
            if (workoutPct >= 0 && nutritionPct >= 0) return (workoutPct + nutritionPct) / 2;
            if (workoutPct >= 0) return workoutPct;
            return nutritionPct;
        }
    }

    @Transactional(readOnly = true)
    public CoachTraineeDetailResponse getTraineeDetails(String email, Long traineeId) {
        Coach coach = findCoachByEmail(email);
        Trainee trainee = traineeRepository.findById(traineeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainee not found"));
        
        if (!trainee.getCoach().getId().equals(coach.getId())) {
            throw new IllegalArgumentException("Not authorized to view this trainee");
        }

        LocalDate today = LocalDate.now();
        LocalDate windowStart = cycleWindowStart(trainee);
        LocalDate windowEnd = today;

        List<PlanAssignment> assignments = planAssignmentRepository.findByTrainee_Id(traineeId);

        int expectedWorkoutCompletions = 0;
        Set<UUID> activeSessionIds = new HashSet<>();
        for (PlanAssignment assignment : assignments) {
            if (assignment.getStartDate() == null || assignment.getPlan() == null) {
                continue;
            }
            LocalDate effectiveStart = assignment.getStartDate().isAfter(windowStart)
                    ? assignment.getStartDate()
                    : windowStart;
            if (effectiveStart.isAfter(windowEnd)) {
                continue;
            }
            List<PlanSession> sessions = assignment.getPlan().getSessions();
            int sessionCount = sessions.size();
            if (sessionCount == 0) {
                continue;
            }
            long overlapDays = ChronoUnit.DAYS.between(effectiveStart, windowEnd) + 1;
            // Workout cadence is "sessions per week" (not "sessions per day"). We prorate when the
            // assignment overlaps a partial week. This matches the existing adherence calculations.
            expectedWorkoutCompletions += (int) Math.round(sessionCount * (overlapDays / 7.0));
            for (PlanSession s : sessions) {
                if (s.getId() != null) {
                    activeSessionIds.add(s.getId());
                }
            }
        }

        int actualWorkoutCompletions = activeSessionIds.isEmpty()
                ? 0
                : (int) traineeWorkoutCompletionRepository
                        .findByTrainee_IdAndCompletionDateBetween(traineeId, windowStart, windowEnd)
                        .stream()
                        .filter(c -> c.getPlanSession() != null
                                && c.getPlanSession().getId() != null
                                && activeSessionIds.contains(c.getPlanSession().getId()))
                        // Count at most 1 completion per (date, session) to avoid inflating numbers.
                        .map(c -> c.getCompletionDate() + "|" + c.getPlanSession().getId())
                        .distinct()
                        .count();

        int missedWorkoutsCount = Math.max(0, expectedWorkoutCompletions - actualWorkoutCompletions);

        List<NutritionPlan> nutritionPlans = nutritionPlanRepository.findByTraineesId(traineeId);
        Set<Long> mealIds = nutritionPlans.stream()
                .flatMap(p -> p.getMeals().stream())
                .map(m -> m.getId())
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        int windowDays = (int) (ChronoUnit.DAYS.between(windowStart, windowEnd) + 1);
        int expectedMealCompletions = mealIds.isEmpty() ? 0 : mealIds.size() * windowDays;
        int actualMealCompletions = mealIds.isEmpty()
                ? 0
                : (int) traineeMealCompletionRepository
                        .findByTraineeIdAndCompletionDateBetween(traineeId, windowStart, windowEnd)
                        .stream()
                        .filter(c -> c.getMeal() != null
                                && c.getMeal().getId() != null
                                && mealIds.contains(c.getMeal().getId()))
                        // Count at most 1 completion per (date, meal).
                        .map(c -> c.getCompletionDate() + "|" + c.getMeal().getId())
                        .distinct()
                        .count();

        int missedMealsCount = Math.max(0, expectedMealCompletions - actualMealCompletions);
        
        return CoachTraineeDetailResponse.builder()
                .profile(toTraineeResponse(trainee, coach))
                .recentMeasurements(
                        measurementService.getTraineeMeasurements(email, traineeId).stream()
                                .limit(5)
                                .collect(Collectors.toList()))
                .recentPictures(
                        measurementService.getTraineeProgressPictures(email, traineeId).stream()
                                .limit(5)
                                .collect(Collectors.toList()))
                .workoutCompletionHistory(
                        exerciseLogService.getWorkoutCompletionHistoryForTrainee(email, traineeId))
                .mealCompletionHistory(
                        getMealCompletionHistoryForTrainee(email, traineeId))
                .missedWorkoutsCount(missedWorkoutsCount)
                .missedMealsCount(missedMealsCount)
                .goals(coachGoalService.getGoalsForTrainee(traineeId))
                .inbodyReports(inBodyReportService.getReportsForTrainee(traineeId))
                .progressPhotos(progressPhotoService.getPhotosForTrainee(traineeId))
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Full meal-completion history for a trainee, newest first. Each entry carries the meal name,
     * its parent nutrition plan, whether it was skipped, and all ingredient-level deviations
     * (skipped or swapped ingredients) so the coach can see exactly what the trainee ate.
     */
    @Transactional(readOnly = true)
    public List<MealCompletionLogResponse> getMealCompletionHistoryForTrainee(String coachEmail, Long traineeId) {
        Coach coach = findCoachByEmail(coachEmail);
        Trainee trainee = traineeRepository.findById(traineeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainee not found"));
        if (!trainee.getCoach().getId().equals(coach.getId())) {
            throw new IllegalArgumentException("Not authorized to view this trainee");
        }

        List<TraineeMealCompletion> completions =
                traineeMealCompletionRepository
                        .findByTraineeIdOrderByCompletionDateDescCompletedAtDesc(traineeId);

        List<Long> completionIds = completions.stream()
                .map(TraineeMealCompletion::getId)
                .collect(Collectors.toList());

        Map<Long, List<MealIngredientDeviation>> deviationsByCompletionId = completionIds.isEmpty()
                ? Map.of()
                : mealIngredientDeviationRepository.findByCompletionIdIn(completionIds)
                        .stream()
                        .collect(Collectors.groupingBy(d -> d.getCompletion().getId()));

        List<MealCompletionLogResponse> result = new ArrayList<>();
        for (TraineeMealCompletion c : completions) {
            List<MealIngredientDeviation> deviations =
                    deviationsByCompletionId.getOrDefault(c.getId(), List.of());

            List<MealCompletionLogResponse.IngredientDeviationItem> items = deviations.stream()
                    .map(d -> {
                        boolean isSwap = d.getReplacementIngredient() != null;
                        return MealCompletionLogResponse.IngredientDeviationItem.builder()
                                .originalIngredientId(d.getOriginalIngredient().getId())
                                .originalIngredientName(d.getOriginalIngredient().getName())
                                .replacementIngredientId(isSwap ? d.getReplacementIngredient().getId() : null)
                                .replacementIngredientName(isSwap ? d.getReplacementIngredient().getName() : null)
                                .newQuantity(d.getNewQuantity())
                                .type(isSwap ? "SWAPPED" : "SKIPPED")
                                .build();
                    })
                    .collect(Collectors.toList());

            NutritionPlan plan = c.getMeal().getNutritionPlan();
            List<MealCompletionLogResponse.PlannedIngredientItem> plannedItems =
                    c.getMeal().getIngredients().stream()
                            .map(ing -> MealCompletionLogResponse.PlannedIngredientItem.builder()
                                    .id(ing.getId())
                                    .name(ing.getName())
                                    .caloriesPer100g(ing.getCalories())
                                    .build())
                            .collect(Collectors.toList());
            result.add(MealCompletionLogResponse.builder()
                    .completionId(c.getId())
                    .mealId(c.getMeal().getId())
                    .mealName(c.getMeal().getName())
                    .nutritionPlanId(plan != null ? plan.getId() : null)
                    .nutritionPlanTitle(plan != null ? plan.getTitle() : null)
                    .completionDate(c.getCompletionDate().toString())
                    .completedAt(c.getCompletedAt())
                    .skipped(c.isSkipped())
                    .hasDeviations(!items.isEmpty())
                    .ingredientDeviations(items)
                    .plannedIngredients(plannedItems)
                    .build());
        }
        return result;
    }

    private Coach findCoachByEmail(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach profile not found"));
    }

    @Transactional
    public CoachProfileResponse uploadAvatar(String email, MultipartFile file) {
        Coach coach = findCoachByEmail(email);
        String url = fileStorageService.store(file, "avatars/coaches/" + coach.getUser().getId());
        coach.getUser().setAvatarUrl(url);
        userRepository.save(coach.getUser());
        return toResponse(coach);
    }

    private CoachProfileResponse toResponse(Coach coach) {
        return CoachProfileResponse.builder()
                .id(coach.getId())
                .userId(coach.getUser().getId())
                .fullName(coach.getUser().getFullName())
                .email(coach.getUser().getEmail())
                .specialisation(coach.getSpecialisation())
                .bio(coach.getBio())
                .traineeCount(coach.getTrainees().size())
                .createdAt(coach.getCreatedAt())
                .avatarUrl(coach.getUser().getAvatarUrl())
                .build();
    }

     
    private TraineeProfileResponse toTraineeResponse(Trainee trainee, Coach coach) {
        LocalDate today = LocalDate.now();
        LocalDate cycleStart = cycleWindowStart(trainee);

        List<PlanAssignment> assignments = planAssignmentRepository.findByTrainee_Id(trainee.getId());

        java.util.Set<UUID> assignedPlanSessionIds = assignments.stream()
                .filter(a -> a.getPlan() != null && a.getPlan().getSessions() != null)
                .flatMap(a -> a.getPlan().getSessions().stream())
                .map(s -> s.getId())
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        List<NutritionPlan> assignedNutritionPlans = nutritionPlanRepository.findByTraineesId(trainee.getId());

        java.util.Set<Long> assignedMealIds = assignedNutritionPlans.stream()
                .flatMap(p -> p.getMeals().stream())
                .map(m -> m.getId())
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        int workoutsPlannedToday = assignedPlanSessionIds.size();
        int workoutsCompletedToday = (int) traineeWorkoutCompletionRepository
                .findByTrainee_IdAndCompletionDate(trainee.getId(), today)
                .stream()
                .filter(c -> c.getPlanSession() != null && c.getPlanSession().getId() != null)
                .map(c -> c.getPlanSession().getId())
                .filter(assignedPlanSessionIds::contains)
                .distinct()
                .count();

        int workoutsCompletedInCycle = (int) traineeWorkoutCompletionRepository
                .findByTrainee_IdAndCompletionDateBetween(trainee.getId(), cycleStart, today)
                .stream()
                .filter(c -> c.getPlanSession() != null
                        && c.getPlanSession().getId() != null
                        && assignedPlanSessionIds.contains(c.getPlanSession().getId()))
                .map(c -> c.getCompletionDate() + "|" + c.getPlanSession().getId())
                .distinct()
                .count();

        int mealsPlannedToday = assignedMealIds.size();
        int mealsCompletedToday = (int) traineeMealCompletionRepository
                .findByTraineeIdAndCompletionDate(trainee.getId(), today)
                .stream()
                .filter(c -> c.getMeal() != null && c.getMeal().getId() != null)
                .map(c -> c.getMeal().getId())
                .filter(assignedMealIds::contains)
                .distinct()
                .count();

        int mealsCompletedInCycle = (int) traineeMealCompletionRepository
                .findByTraineeIdAndCompletionDateBetween(trainee.getId(), cycleStart, today)
                .stream()
                .filter(c -> c.getMeal() != null
                        && c.getMeal().getId() != null
                        && assignedMealIds.contains(c.getMeal().getId()))
                .map(c -> c.getCompletionDate() + "|" + c.getMeal().getId())
                .distinct()
                .count();

        int daysInCycle = (int) (ChronoUnit.DAYS.between(cycleStart, today) + 1);

        int expectedWorkoutsInCycle = 0;
        for (PlanAssignment a : assignments) {
            if (a.getStartDate() == null || a.getPlan() == null) {
                continue;
            }
            LocalDate effectiveStart = a.getStartDate().isAfter(cycleStart) ? a.getStartDate() : cycleStart;
            if (effectiveStart.isAfter(today)) {
                continue;
            }
            List<PlanSession> sessions = a.getPlan().getSessions();
            int sessionCount = sessions.size();
            if (sessionCount == 0) {
                continue;
            }
            long overlapDays = ChronoUnit.DAYS.between(effectiveStart, today) + 1;
            expectedWorkoutsInCycle += (int) Math.round(sessionCount * (overlapDays / 7.0));
        }

        int expectedMealsInCycle = assignedMealIds.isEmpty() ? 0 : assignedMealIds.size() * daysInCycle;

        int workoutProgressPercent = expectedWorkoutsInCycle == 0
                ? 0
                : (int) Math.round((workoutsCompletedInCycle * 100.0) / expectedWorkoutsInCycle);
        int nutritionProgressPercent = expectedMealsInCycle == 0
                ? 0
                : (int) Math.round((mealsCompletedInCycle * 100.0) / expectedMealsInCycle);
        int adherencePercent = (workoutProgressPercent + nutritionProgressPercent) / 2;

        int missedWorkoutsCount = Math.max(0, expectedWorkoutsInCycle - workoutsCompletedInCycle);
        int missedMealsCount = Math.max(0, expectedMealsInCycle - mealsCompletedInCycle);

        return TraineeProfileResponse.builder()
                .id(trainee.getId())
                .userId(trainee.getUser().getId())
                .fullName(trainee.getUser().getFullName())
                .email(trainee.getUser().getEmail())
                .fitnessGoal(trainee.getFitnessGoal())
                .traineeLevel(trainee.getTraineeLevel())
                .coachId(coach.getId())
                .coachName(coach.getUser().getFullName())
                .createdAt(trainee.getCreatedAt())
                .workoutsCompletedToday(workoutsCompletedToday)
                .workoutsPlannedToday(workoutsPlannedToday)
                .mealsCompletedToday(mealsCompletedToday)
                .mealsPlannedToday(mealsPlannedToday)
                .workoutProgressPercent(Math.min(100, Math.max(0, workoutProgressPercent)))
                .nutritionProgressPercent(Math.min(100, Math.max(0, nutritionProgressPercent)))
                .adherencePercent(Math.min(100, Math.max(0, adherencePercent)))
                .missedWorkoutsCount(missedWorkoutsCount)
                .missedMealsCount(missedMealsCount)
                .coachFeedback(trainee.getCoachFeedback())
                .cautionNotes(trainee.getCautionNotes())
                .currentStreak(trainee.getCurrentStreak())
                .avatarUrl(trainee.getUser().getAvatarUrl())
                .build();
    }

    @Transactional
    public TraineeProfileResponse archiveTrainee(String coachEmail, Long traineeId) {
        Coach coach = findCoachByEmail(coachEmail);
        Trainee trainee = traineeRepository.findById(traineeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainee not found"));
        if (!trainee.getCoach().getId().equals(coach.getId())) {
            throw new IllegalArgumentException("Not authorized to archive this trainee");
        }
        trainee.setStatus(TraineeStatus.ARCHIVED);
        traineeRepository.save(trainee);
        return toTraineeResponse(trainee, coach);
    }

    @Transactional
    public void deleteTrainee(String coachEmail, Long traineeId) {
        Coach coach = findCoachByEmail(coachEmail);
        Trainee trainee = traineeRepository.findById(traineeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainee not found"));
        if (!trainee.getCoach().getId().equals(coach.getId())) {
            throw new IllegalArgumentException("Not authorized to delete this trainee");
        }
        trainee.setStatus(TraineeStatus.DELETED);
        traineeRepository.save(trainee);
    }
}
