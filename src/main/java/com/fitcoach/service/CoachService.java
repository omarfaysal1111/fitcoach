package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.NutritionPlan;
import com.fitcoach.domain.entity.PlanAssignment;
import com.fitcoach.domain.entity.PlanSession;
import com.fitcoach.domain.entity.PlanStatus;
import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.dto.request.UpdateCoachRequest;
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
    private final ExerciseLogService exerciseLogService;
    private final MeasurementService measurementService;

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

    @Transactional(readOnly = true)
    public List<TraineeProfileResponse> getMyTrainees(String email) {
        Coach coach = findCoachByEmail(email);
        return traineeRepository.findAllByCoachId(coach.getId())
                .stream()
                .map(t -> toTraineeResponse(t, coach))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CoachAlertResponse> getAlerts(String email) {
        Coach coach = findCoachByEmail(email);
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(6);
        LocalDate windowEnd = today;

        List<Trainee> trainees = traineeRepository.findAllByCoachId(coach.getId());
        record PendingAlert(Trainee trainee, Adherence7d adherence) {}
        List<PendingAlert> pending = new ArrayList<>();

        for (Trainee trainee : trainees) {
            Adherence7d a = computeAdherence7d(trainee.getId(), windowStart, windowEnd);
            if (!a.hasAssignablePlans() || a.combinedPercent() >= 100) {
                continue;
            }
            pending.add(new PendingAlert(trainee, a));
        }

        pending.sort(Comparator.comparingInt(p -> p.adherence().combinedPercent()));

        List<CoachAlertResponse> alerts = new ArrayList<>();
        for (PendingAlert p : pending) {
            alerts.add(CoachAlertResponse.builder()
                    .id("low-adherence-" + p.trainee().getId())
                    .traineeId(String.valueOf(p.trainee().getId()))
                    .traineeName(p.trainee().getUser().getFullName())
                    .message(formatAdherenceAlertMessage(p.adherence()))
                    .type("low_adherence")
                    .build());
        }
        return alerts;
    }

    /**
     * Rolling 7-day adherence: workouts from active assignments (expected sessions/week prorated by
     * overlap with the window and {@link PlanAssignment#getStartDate()}), nutrition from all assigned
     * plans (each meal once per day in the window). Combined score is the average of workout and
     * nutrition percentages when both apply; otherwise the single applicable score.
     */
    private Adherence7d computeAdherence7d(Long traineeId, LocalDate windowStart, LocalDate windowEnd) {
        double workoutExpected = 0;
        Set<UUID> workoutSessionIds = new HashSet<>();
        List<PlanAssignment> activeAssignments =
                planAssignmentRepository.findByTrainee_IdAndStatus(traineeId, PlanStatus.ACTIVE);

        for (PlanAssignment assignment : activeAssignments) {
            LocalDate effectiveStart = assignment.getStartDate().isAfter(windowStart)
                    ? assignment.getStartDate()
                    : windowStart;
            if (effectiveStart.isAfter(windowEnd)) {
                continue;
            }
            long overlapDays = ChronoUnit.DAYS.between(effectiveStart, windowEnd) + 1;
            List<PlanSession> sessions = assignment.getPlan().getSessions();
            int sessionCount = sessions.size();
            if (sessionCount == 0) {
                continue;
            }
            workoutExpected += sessionCount * (overlapDays / 7.0);
            for (PlanSession s : sessions) {
                if (s.getId() != null) {
                    workoutSessionIds.add(s.getId());
                }
            }
        }

        Map<UUID, Long> completionsPerSession = new HashMap<>();
        if (!workoutSessionIds.isEmpty()) {
            traineeWorkoutCompletionRepository
                    .findByTrainee_IdAndCompletionDateBetween(traineeId, windowStart, windowEnd)
                    .stream()
                    .filter(c -> workoutSessionIds.contains(c.getPlanSession().getId()))
                    .forEach(c -> completionsPerSession.merge(c.getPlanSession().getId(), 1L, Long::sum));
        }

        double workoutActual = 0;
        for (UUID sid : workoutSessionIds) {
            long n = completionsPerSession.getOrDefault(sid, 0L);
            workoutActual += Math.min(1L, n);
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

        return new Adherence7d(workoutPct, nutritionPct);
    }

    private static String formatAdherenceAlertMessage(Adherence7d a) {
        if (a.workoutPct >= 0 && a.nutritionPct >= 0) {
            return "Last 7 days: " + a.combinedPercent()
                    + "% combined adherence (workouts " + a.workoutPct
                    + "%, nutrition " + a.nutritionPct + "%).";
        }
        if (a.workoutPct >= 0) {
            return "Last 7 days: " + a.workoutPct + "% workout adherence.";
        }
        return "Last 7 days: " + a.nutritionPct + "% nutrition adherence.";
    }

    private static final class Adherence7d {
        private final int workoutPct;
        private final int nutritionPct;

        Adherence7d(int workoutPct, int nutritionPct) {
            this.workoutPct = workoutPct;
            this.nutritionPct = nutritionPct;
        }

        boolean hasAssignablePlans() {
            return workoutPct >= 0 || nutritionPct >= 0;
        }

        int combinedPercent() {
            if (workoutPct >= 0 && nutritionPct >= 0) {
                return (workoutPct + nutritionPct) / 2;
            }
            if (workoutPct >= 0) {
                return workoutPct;
            }
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
        LocalDate joinedAt = trainee.getCreatedAt() == null ? today : trainee.getCreatedAt().toLocalDate();
        LocalDate windowStart = joinedAt;
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
                .missedWorkoutsCount(missedWorkoutsCount)
                .missedMealsCount(missedMealsCount)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Coach findCoachByEmail(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach profile not found"));
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
                .build();
    }

    private TraineeProfileResponse toTraineeResponse(Trainee trainee, Coach coach) {
        LocalDate today = LocalDate.now();
        LocalDate joinedAt = trainee.getCreatedAt() == null ? today : trainee.getCreatedAt().toLocalDate();

        List<PlanAssignment> assignments = planAssignmentRepository.findByTrainee_Id(trainee.getId());

        java.util.Set<UUID> assignedPlanSessionIds = assignments.stream()
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
                .map(c -> c.getPlanSession().getId())
                .filter(assignedPlanSessionIds::contains)
                .distinct()
                .count();

        int workoutsCompletedSinceJoin = (int) traineeWorkoutCompletionRepository
                .findByTrainee_IdAndCompletionDateBetween(trainee.getId(), joinedAt, today)
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
                .map(c -> c.getMeal().getId())
                .filter(assignedMealIds::contains)
                .distinct()
                .count();

        int mealsCompletedSinceJoin = (int) traineeMealCompletionRepository
                .findByTraineeIdAndCompletionDateBetween(trainee.getId(), joinedAt, today)
                .stream()
                .filter(c -> c.getMeal() != null
                        && c.getMeal().getId() != null
                        && assignedMealIds.contains(c.getMeal().getId()))
                .map(c -> c.getCompletionDate() + "|" + c.getMeal().getId())
                .distinct()
                .count();

        int daysSinceJoin = (int) (ChronoUnit.DAYS.between(joinedAt, today) + 1);

        int expectedWorkoutsSinceJoin = 0;
        for (PlanAssignment a : assignments) {
            if (a.getStartDate() == null || a.getPlan() == null) {
                continue;
            }
            LocalDate effectiveStart = a.getStartDate().isAfter(joinedAt) ? a.getStartDate() : joinedAt;
            if (effectiveStart.isAfter(today)) {
                continue;
            }
            List<PlanSession> sessions = a.getPlan().getSessions();
            int sessionCount = sessions.size();
            if (sessionCount == 0) {
                continue;
            }
            long overlapDays = ChronoUnit.DAYS.between(effectiveStart, today) + 1;
            expectedWorkoutsSinceJoin += (int) Math.round(sessionCount * (overlapDays / 7.0));
        }

        int expectedMealsSinceJoin = assignedMealIds.isEmpty() ? 0 : assignedMealIds.size() * daysSinceJoin;

        int workoutProgressPercent = expectedWorkoutsSinceJoin == 0
                ? 0
                : (int) Math.round((workoutsCompletedSinceJoin * 100.0) / expectedWorkoutsSinceJoin);
        int nutritionProgressPercent = expectedMealsSinceJoin == 0
                ? 0
                : (int) Math.round((mealsCompletedSinceJoin * 100.0) / expectedMealsSinceJoin);
        int adherencePercent = (workoutProgressPercent + nutritionProgressPercent) / 2;

        return TraineeProfileResponse.builder()
                .id(trainee.getId())
                .userId(trainee.getUser().getId())
                .fullName(trainee.getUser().getFullName())
                .email(trainee.getUser().getEmail())
                .fitnessGoal(trainee.getFitnessGoal())
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
                .build();
    }
}
