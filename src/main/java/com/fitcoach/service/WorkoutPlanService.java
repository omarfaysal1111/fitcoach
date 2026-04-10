package com.fitcoach.service;

import com.fitcoach.domain.entity.*;
import com.fitcoach.dto.request.plan.AddWorkoutDayRequest;
import com.fitcoach.dto.request.plan.AssignPlanToTraineeRequest;
import com.fitcoach.dto.request.plan.CreatePlanRequest;
import com.fitcoach.dto.request.plan.WorkoutExerciseLineRequest;
import com.fitcoach.dto.response.plan.PlanAssignmentResponse;
import com.fitcoach.dto.response.plan.WorkoutDayResponse;
import com.fitcoach.dto.response.plan.WorkoutExerciseLineResponse;
import com.fitcoach.dto.response.plan.WorkoutPlanResponse;
import com.fitcoach.exception.ConflictException;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutPlanService {

    private final WorkoutPlanRepository workoutPlanRepository;
    private final PlanSessionRepository planSessionRepository;
    private final PlanSessionExerciseRepository planSessionExerciseRepository;
    private final PlanAssignmentRepository planAssignmentRepository;
    private final CoachRepository coachRepository;
    private final TraineeRepository traineeRepository;
    private final ExerciseRepository exerciseRepository;

    @Transactional
    public WorkoutPlanResponse createPlan(CreatePlanRequest request, Long authenticatedCoachId) {
        if (!request.getCoachId().equals(authenticatedCoachId)) {
            throw new AccessDeniedException("coachId must match the authenticated coach");
        }
        Coach coach = coachRepository.findById(request.getCoachId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));

        WorkoutPlan plan = WorkoutPlan.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .coach(coach)
                .build();

        return toWorkoutPlanResponse(workoutPlanRepository.save(plan));
    }

    @Transactional
    public WorkoutDayResponse addWorkoutDay(UUID planId, Long coachId, AddWorkoutDayRequest request) {
        WorkoutPlan plan = workoutPlanRepository.findByIdAndCoach_Id(planId, coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout plan not found"));

        PlanSession session = PlanSession.builder()
                .plan(plan)
                .title(request.getTitle().trim())
                .dayOrder(request.getDayOrder())
                .build();
        plan.getSessions().add(session);
        planSessionRepository.saveAndFlush(session);

        return toWorkoutDayResponse(session);
    }

    /**
     * Replaces all exercise lines for the plan session (API "workout") with the given list.
     */
    @Transactional
    public List<WorkoutExerciseLineResponse> replaceWorkoutExercises(
            UUID workoutId,
            Long coachId,
            List<WorkoutExerciseLineRequest> lines) {

        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("At least one exercise line is required");
        }

        PlanSession session = planSessionRepository.findById(workoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout (plan session) not found"));

        if (!session.getPlan().getCoach().getId().equals(coachId)) {
            throw new AccessDeniedException("You do not own this workout plan");
        }

        planSessionExerciseRepository.deleteByPlanSession_Id(session.getId());

        List<PlanSessionExercise> built = new ArrayList<>();
        for (WorkoutExerciseLineRequest line : lines) {
            Exercise exercise = exerciseRepository.findById(line.getExerciseId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Exercise not found with id: " + line.getExerciseId()));

            built.add(PlanSessionExercise.builder()
                    .planSession(session)
                    .exercise(exercise)
                    .sectionType(line.getSectionType())
                    .orderIndex(line.getOrderIndex())
                    .sets(line.getSets())
                    .reps(line.getReps() != null ? line.getReps() : "")
                    .loadAmount(toBigDecimal(line.getLoadAmount()))
                    .restSeconds(line.getRestSeconds())
                    .build());
        }

        return planSessionExerciseRepository.saveAll(built).stream()
                .map(this::toWorkoutExerciseLineResponse)
                .toList();
    }

    @Transactional
    public PlanAssignmentResponse assignPlanToTrainee(
            UUID planId,
            Long coachId,
            AssignPlanToTraineeRequest request) {

        WorkoutPlan plan = workoutPlanRepository.findByIdAndCoach_Id(planId, coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout plan not found"));

        Trainee trainee = traineeRepository.findById(request.getTraineeId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainee not found"));

        if (!trainee.getCoach().getId().equals(coachId)) {
            throw new ResourceNotFoundException("Trainee not found for this coach");
        }

        if (planAssignmentRepository.existsByPlan_IdAndTrainee_Id(plan.getId(), trainee.getId())) {
            throw new ConflictException("Trainee is already assigned to this plan");
        }

        PlanAssignment assignment = PlanAssignment.builder()
                .plan(plan)
                .trainee(trainee)
                .startDate(request.getStartDate())
                .status(PlanStatus.ACTIVE)
                .build();
        plan.getAssignments().add(assignment);
        planAssignmentRepository.saveAndFlush(assignment);

        return toPlanAssignmentResponse(assignment);
    }

    private static BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value);
    }

    private WorkoutPlanResponse toWorkoutPlanResponse(WorkoutPlan plan) {
        return WorkoutPlanResponse.builder()
                .id(plan.getId())
                .coachId(plan.getCoach().getId())
                .title(plan.getTitle())
                .description(plan.getDescription())
                .createdAt(plan.getCreatedAt())
                .build();
    }

    private WorkoutDayResponse toWorkoutDayResponse(PlanSession session) {
        return WorkoutDayResponse.builder()
                .id(session.getId())
                .planId(session.getPlan().getId())
                .title(session.getTitle())
                .dayOrder(session.getDayOrder())
                .build();
    }

    private WorkoutExerciseLineResponse toWorkoutExerciseLineResponse(PlanSessionExercise pse) {
        return WorkoutExerciseLineResponse.builder()
                .id(pse.getId())
                .workoutId(pse.getPlanSession().getId())
                .exerciseId(pse.getExercise().getId())
                .sectionType(pse.getSectionType())
                .orderIndex(pse.getOrderIndex())
                .sets(pse.getSets())
                .reps(pse.getReps())
                .loadAmount(pse.getLoadAmount())
                .restSeconds(pse.getRestSeconds())
                .build();
    }

    private PlanAssignmentResponse toPlanAssignmentResponse(PlanAssignment a) {
        return PlanAssignmentResponse.builder()
                .id(a.getId())
                .planId(a.getPlan().getId())
                .traineeId(a.getTrainee().getId())
                .startDate(a.getStartDate())
                .status(a.getStatus())
                .build();
    }
}
