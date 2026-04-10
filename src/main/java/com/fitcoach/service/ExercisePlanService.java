package com.fitcoach.service;

import com.fitcoach.domain.entity.*;
import com.fitcoach.dto.request.CreateExercisePlanRequest;
import com.fitcoach.dto.request.CreatePlanSessionRequest;
import com.fitcoach.dto.request.WorkoutExerciseItemRequest;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExercisePlanService {

    private final WorkoutPlanRepository workoutPlanRepository;
    private final PlanAssignmentRepository planAssignmentRepository;
    private final CoachRepository coachRepository;
    private final TraineeRepository traineeRepository;
    private final ExerciseRepository exerciseRepository;

    @Transactional
    public WorkoutPlan createExercisePlan(Long coachId, CreateExercisePlanRequest request) {
        Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));

        WorkoutPlan plan = WorkoutPlan.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .coach(coach)
                .build();

        if (request.getSessions() != null && !request.getSessions().isEmpty()) {
            int dayOrder = 1;
            for (CreatePlanSessionRequest sessionReq : request.getSessions()) {
                plan.getSessions().add(createPlanSession(sessionReq, plan, dayOrder++));
            }
        }

        WorkoutPlan saved = workoutPlanRepository.save(plan);

        if (request.getTraineeIds() != null && !request.getTraineeIds().isEmpty()) {
            List<Trainee> trainees = traineeRepository.findAllById(request.getTraineeIds());
            if (trainees.size() != request.getTraineeIds().size()) {
                throw new ResourceNotFoundException("One or more trainees not found");
            }
            LocalDate start = LocalDate.now();
            for (Trainee t : trainees) {
                if (!planAssignmentRepository.existsByPlan_IdAndTrainee_Id(saved.getId(), t.getId())) {
                    saved.getAssignments().add(PlanAssignment.builder()
                            .plan(saved)
                            .trainee(t)
                            .startDate(start)
                            .status(PlanStatus.ACTIVE)
                            .build());
                }
            }
            saved = workoutPlanRepository.save(saved);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<WorkoutPlan> getPlansByCoach(Long coachId) {
        return workoutPlanRepository.findByCoach_Id(coachId);
    }

    @Transactional(readOnly = true)
    public List<WorkoutPlan> getPlansByTrainee(Long traineeId) {
        return planAssignmentRepository.findByTrainee_Id(traineeId).stream()
                .map(PlanAssignment::getPlan)
                .collect(Collectors.toMap(WorkoutPlan::getId, p -> p, (a, b) -> a))
                .values()
                .stream()
                .toList();
    }

    @Transactional
    public WorkoutPlan assignPlanToTrainees(UUID planId, Long coachId, List<Long> traineeIds) {
        WorkoutPlan plan = workoutPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout plan not found"));

        if (!plan.getCoach().getId().equals(coachId)) {
            throw new IllegalArgumentException("You do not have permission to modify this plan");
        }

        List<Trainee> newTrainees = traineeRepository.findAllById(traineeIds);
        if (newTrainees.size() != traineeIds.size()) {
            throw new ResourceNotFoundException("One or more trainees not found");
        }

        LocalDate start = LocalDate.now();
        for (Trainee t : newTrainees) {
            if (!planAssignmentRepository.existsByPlan_IdAndTrainee_Id(plan.getId(), t.getId())) {
                plan.getAssignments().add(PlanAssignment.builder()
                        .plan(plan)
                        .trainee(t)
                        .startDate(start)
                        .status(PlanStatus.ACTIVE)
                        .build());
            }
        }

        return workoutPlanRepository.save(plan);
    }

    private PlanSession createPlanSession(CreatePlanSessionRequest request, WorkoutPlan plan, int dayOrder) {
        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            title = "Session " + dayOrder;
        }

        PlanSession session = PlanSession.builder()
                .title(title.trim())
                .dayOrder(dayOrder)
                .plan(plan)
                .build();

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (WorkoutExerciseItemRequest item : request.getItems()) {
                Exercise exercise = exerciseRepository.findById(item.getExerciseId())
                        .orElseThrow(() -> new ResourceNotFoundException("Exercise not found with id: " + item.getExerciseId()));

                PlanSessionExercise pse = PlanSessionExercise.builder()
                        .planSession(session)
                        .exercise(exercise)
                        .sectionType(parseSectionType(item.getSectionType()))
                        .orderIndex(item.getOrder())
                        .sets(item.getSets())
                        .reps(item.getReps() != null ? item.getReps() : "")
                        .loadAmount(parseLoadAmount(item.getLoad()))
                        .restSeconds(parseRestSeconds(item.getRest()))
                        .build();

                session.getSessionExercises().add(pse);
            }
        } else if (request.getExerciseIds() != null && !request.getExerciseIds().isEmpty()) {
            int order = 1;
            for (Long exerciseId : request.getExerciseIds()) {
                Exercise exercise = exerciseRepository.findById(exerciseId)
                        .orElseThrow(() -> new ResourceNotFoundException("Exercise not found with id: " + exerciseId));

                PlanSessionExercise pse = PlanSessionExercise.builder()
                        .planSession(session)
                        .exercise(exercise)
                        .sectionType(SectionType.MAIN)
                        .orderIndex(order++)
                        .sets(3)
                        .reps("10-12")
                        .loadAmount(null)
                        .restSeconds(60)
                        .build();

                session.getSessionExercises().add(pse);
            }
        } else {
            throw new IllegalArgumentException("Each plan session must include at least one exercise item");
        }

        return session;
    }

    private static SectionType parseSectionType(String raw) {
        if (raw == null || raw.isBlank()) {
            return SectionType.MAIN;
        }
        try {
            return SectionType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SectionType.MAIN;
        }
    }

    private static BigDecimal parseLoadAmount(String load) {
        if (load == null || load.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(load.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseRestSeconds(String rest) {
        if (rest == null || rest.isBlank()) {
            return null;
        }
        String s = rest.trim().toLowerCase().replace("s", "").trim();
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
