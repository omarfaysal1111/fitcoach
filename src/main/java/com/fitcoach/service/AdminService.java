package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.Ingredient;
import com.fitcoach.domain.entity.Meal;
import com.fitcoach.domain.entity.NutritionPlan;
import com.fitcoach.domain.entity.PaymentRecord;
import com.fitcoach.domain.entity.PlanAssignment;
import com.fitcoach.domain.entity.PlanSession;
import com.fitcoach.domain.entity.PlanSessionExercise;
import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.domain.entity.User;
import com.fitcoach.domain.enums.PaymentStatus;
import com.fitcoach.domain.enums.TraineeStatus;
import com.fitcoach.dto.response.admin.AdminCoachDetailResponse;
import com.fitcoach.dto.response.admin.AdminCoachSummaryResponse;
import com.fitcoach.dto.response.admin.AdminPaymentResponse;
import com.fitcoach.dto.response.admin.AdminStatsResponse;
import com.fitcoach.dto.response.admin.AdminTraineePlansResponse;
import com.fitcoach.dto.response.admin.AdminTraineeResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.NutritionPlanRepository;
import com.fitcoach.repository.PaymentRecordRepository;
import com.fitcoach.repository.PlanAssignmentRepository;
import com.fitcoach.repository.TraineeRepository;
import com.fitcoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final CoachRepository coachRepository;
    private final TraineeRepository traineeRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final UserRepository userRepository;
    private final PlanAssignmentRepository planAssignmentRepository;
    private final NutritionPlanRepository nutritionPlanRepository;

    // ─────────────────────────────── Stats ───────────────────────────────────

    /**
     * GET /admin/stats
     * Platform-wide aggregated numbers for the admin dashboard home.
     */
    public AdminStatsResponse getStats() {
        long totalCoaches   = coachRepository.count();
        long totalTrainees  = traineeRepository.count();
        long activeTrainees = traineeRepository.countByStatus(TraineeStatus.ACTIVE);
        long totalPayments  = paymentRecordRepository.count();
        long pendingPayments  = paymentRecordRepository.countByStatus(PaymentStatus.PENDING);
        long approvedPayments = paymentRecordRepository.countByStatus(PaymentStatus.APPROVED);
        long rejectedPayments = paymentRecordRepository.countByStatus(PaymentStatus.REJECTED);

        return AdminStatsResponse.builder()
                .totalCoaches(totalCoaches)
                .totalTrainees(totalTrainees)
                .activeTrainees(activeTrainees)
                .totalPayments(totalPayments)
                .pendingPayments(pendingPayments)
                .approvedPayments(approvedPayments)
                .rejectedPayments(rejectedPayments)
                .totalApprovedRevenue(
                        paymentRecordRepository.sumClaimedAmountByStatus(PaymentStatus.APPROVED))
                .build();
    }

    // ─────────────────────────────── Coaches ─────────────────────────────────

    /**
     * GET /admin/coaches
     * Summary list of every coach with their trainee counts.
     */
    public List<AdminCoachSummaryResponse> getAllCoaches() {
        return coachRepository.findAll()
                .stream()
                .map(this::toCoachSummary)
                .toList();
    }

    /**
     * GET /admin/coaches/{coachId}
     * Full profile of a single coach including their complete trainee roster.
     */
    public AdminCoachDetailResponse getCoachDetail(Long coachId) {
        Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found with id: " + coachId));

        List<Trainee> trainees = traineeRepository.findAllByCoachId(coachId);
        long activeCount = trainees.stream()
                .filter(t -> TraineeStatus.ACTIVE.equals(t.getStatus()))
                .count();

        return AdminCoachDetailResponse.builder()
                .coachId(coach.getId())
                .fullName(coach.getUser().getFullName())
                .email(coach.getUser().getEmail())
                .specialisation(coach.getSpecialisation())
                .bio(coach.getBio())
                .totalTrainees(trainees.size())
                .activeTrainees(activeCount)
                .memberSince(coach.getCreatedAt())
                .trainees(trainees.stream().map(this::toTraineeResponse).toList())
                .build();
    }

    // ─────────────────────────────── Trainees ────────────────────────────────

    /**
     * GET /admin/coaches/{coachId}/trainees
     * All trainees that belong to a specific coach.
     */
    public List<AdminTraineeResponse> getTraineesForCoach(Long coachId) {
        if (!coachRepository.existsById(coachId)) {
            throw new ResourceNotFoundException("Coach not found with id: " + coachId);
        }
        return traineeRepository.findAllByCoachId(coachId)
                .stream()
                .map(this::toTraineeResponse)
                .toList();
    }

    /**
     * GET /admin/trainees/plans?email={email}
     * Every plan (workout + nutrition) assigned to the trainee with the given email.
     */
    public AdminTraineePlansResponse getPlansForTraineeByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No user found with email: " + email));
        Trainee trainee = traineeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No trainee found with email: " + email));

        List<AdminTraineePlansResponse.WorkoutPlanItem> workoutPlans =
                planAssignmentRepository.findByTrainee_Id(trainee.getId())
                        .stream()
                        .map(this::toWorkoutPlanItem)
                        .toList();

        List<AdminTraineePlansResponse.NutritionPlanItem> nutritionPlans =
                nutritionPlanRepository.findByTraineesId(trainee.getId())
                        .stream()
                        .map(this::toNutritionPlanItem)
                        .toList();

        return AdminTraineePlansResponse.builder()
                .traineeId(trainee.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .workoutPlans(workoutPlans)
                .nutritionPlans(nutritionPlans)
                .build();
    }

    // ─────────────────────────────── Payments ────────────────────────────────

    /**
     * GET /admin/payments
     * All payment records across every coach, newest first.
     */
    public List<AdminPaymentResponse> getAllPayments() {
        return paymentRecordRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    /**
     * GET /admin/coaches/{coachId}/payments
     * All payment records submitted by a specific coach, newest first.
     */
    public List<AdminPaymentResponse> getPaymentsForCoach(Long coachId) {
        if (!coachRepository.existsById(coachId)) {
            throw new ResourceNotFoundException("Coach not found with id: " + coachId);
        }
        return paymentRecordRepository.findAllByCoachIdOrderByCreatedAtDesc(coachId)
                .stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    // ─────────────────────────────── Mappers ─────────────────────────────────

    private AdminCoachSummaryResponse toCoachSummary(Coach coach) {
        long total  = traineeRepository.findAllByCoachId(coach.getId()).size();
        long active = traineeRepository.countByCoachIdAndStatus(coach.getId(), TraineeStatus.ACTIVE);

        return AdminCoachSummaryResponse.builder()
                .coachId(coach.getId())
                .fullName(coach.getUser().getFullName())
                .email(coach.getUser().getEmail())
                .specialisation(coach.getSpecialisation())
                .totalTrainees(total)
                .activeTrainees(active)
                .memberSince(coach.getCreatedAt())
                .build();
    }

    private AdminTraineeResponse toTraineeResponse(Trainee trainee) {
        return AdminTraineeResponse.builder()
                .traineeId(trainee.getId())
                .fullName(trainee.getUser().getFullName())
                .email(trainee.getUser().getEmail())
                .status(trainee.getStatus())
                .fitnessGoal(trainee.getFitnessGoal())
                .traineeLevel(trainee.getTraineeLevel())
                .currentStreak(trainee.getCurrentStreak())
                .joinedAt(trainee.getCreatedAt())
                .build();
    }

    private AdminTraineePlansResponse.WorkoutPlanItem toWorkoutPlanItem(PlanAssignment assignment) {
        var plan = assignment.getPlan();
        List<AdminTraineePlansResponse.SessionItem> sessions = plan.getSessions().stream()
                .sorted(Comparator.comparing(PlanSession::getDayOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toSessionItem)
                .toList();

        return AdminTraineePlansResponse.WorkoutPlanItem.builder()
                .planId(plan.getId())
                .title(plan.getTitle())
                .description(plan.getDescription())
                .coachName(plan.getCoach().getUser().getFullName())
                .startDate(assignment.getStartDate())
                .status(assignment.getStatus())
                .createdAt(plan.getCreatedAt())
                .sessions(sessions)
                .build();
    }

    private AdminTraineePlansResponse.SessionItem toSessionItem(PlanSession session) {
        List<AdminTraineePlansResponse.ExerciseItem> exercises = session.getSessionExercises().stream()
                .sorted(Comparator.comparingInt(PlanSessionExercise::getOrderIndex))
                .map(this::toExerciseItem)
                .toList();

        return AdminTraineePlansResponse.SessionItem.builder()
                .sessionId(session.getId())
                .title(session.getTitle())
                .dayOrder(session.getDayOrder())
                .exercises(exercises)
                .build();
    }

    private AdminTraineePlansResponse.ExerciseItem toExerciseItem(PlanSessionExercise pse) {
        var exercise = pse.getExercise();
        return AdminTraineePlansResponse.ExerciseItem.builder()
                .id(pse.getId())
                .orderIndex(pse.getOrderIndex())
                .name(exercise.getName())
                .targetedMuscle(exercise.getTargetedMuscle())
                .videoLink(exercise.getVideoLink())
                .sectionType(pse.getSectionType() != null ? pse.getSectionType().name() : null)
                .sets(pse.getSets())
                .reps(pse.getReps())
                .loadAmount(pse.getLoadAmount())
                .restSeconds(pse.getRestSeconds())
                .build();
    }

    private AdminTraineePlansResponse.NutritionPlanItem toNutritionPlanItem(NutritionPlan plan) {
        List<AdminTraineePlansResponse.MealItem> meals = plan.getMeals().stream()
                .map(this::toMealItem)
                .toList();

        return AdminTraineePlansResponse.NutritionPlanItem.builder()
                .planId(plan.getId())
                .title(plan.getTitle())
                .description(plan.getDescription())
                .coachName(plan.getCoach().getUser().getFullName())
                .waterTargetLiters(plan.getWaterTargetLiters())
                .createdAt(plan.getCreatedAt())
                .meals(meals)
                .build();
    }

    private AdminTraineePlansResponse.MealItem toMealItem(Meal meal) {
        List<AdminTraineePlansResponse.IngredientItem> ingredients = meal.getIngredients().stream()
                .map(this::toIngredientItem)
                .toList();

        return AdminTraineePlansResponse.MealItem.builder()
                .id(meal.getId())
                .name(meal.getName())
                .calories(meal.getCalories())
                .protein(meal.getProtein())
                .carbs(meal.getCarbs())
                .fat(meal.getFat())
                .ingredients(ingredients)
                .build();
    }

    private AdminTraineePlansResponse.IngredientItem toIngredientItem(Ingredient ingredient) {
        return AdminTraineePlansResponse.IngredientItem.builder()
                .id(ingredient.getId())
                .name(ingredient.getName())
                .servingQuantityG(ingredient.getServingQuantityG())
                .calories(ingredient.getCalories())
                .protein(ingredient.getProtein())
                .carbohydrates(ingredient.getCarbohydrates())
                .fat(ingredient.getFat())
                .build();
    }

    private AdminPaymentResponse toPaymentResponse(PaymentRecord record) {
        Coach coach = record.getCoach();
        return AdminPaymentResponse.builder()
                .paymentId(record.getId())
                .coachId(coach.getId())
                .coachName(coach.getUser().getFullName())
                .coachEmail(coach.getUser().getEmail())
                .desiredPlan(record.getDesiredPlan())
                .paymentMethod(record.getPaymentMethod())
                .claimedAmount(record.getClaimedAmount())
                .ocrExtractedAmount(record.getOcrExtractedAmount())
                .status(record.getStatus())
                .reviewNote(record.getReviewNote())
                .screenshotPath(record.getScreenshotPath())
                .submittedAt(record.getCreatedAt())
                .build();
    }
}
