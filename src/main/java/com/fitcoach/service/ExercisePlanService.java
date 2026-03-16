package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.Exercise;
import com.fitcoach.domain.entity.ExercisePlan;
import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.domain.entity.Workout;
import com.fitcoach.domain.entity.WorkoutExercise;
import com.fitcoach.dto.request.CreateExercisePlanRequest;
import com.fitcoach.dto.request.CreateWorkoutRequest;
import com.fitcoach.dto.request.WorkoutExerciseItemRequest;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.ExercisePlanRepository;
import com.fitcoach.repository.ExerciseRepository;
import com.fitcoach.repository.TraineeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExercisePlanService {

    private final ExercisePlanRepository exercisePlanRepository;
    private final CoachRepository coachRepository;
    private final TraineeRepository traineeRepository;
    private final ExerciseRepository exerciseRepository;

    @Transactional
    public ExercisePlan createExercisePlan(Long coachId, CreateExercisePlanRequest request) {
        Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));

        List<Trainee> trainees = traineeRepository.findAllById(request.getTraineeIds());
        
        // Ensure all trainees exist
        if (trainees.size() != request.getTraineeIds().size()) {
            throw new ResourceNotFoundException("One or more trainees not found");
        }

        ExercisePlan plan = ExercisePlan.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .coach(coach)
                .trainees(trainees)
                .build();

        if (request.getWorkouts() != null) {
            List<Workout> workouts = request.getWorkouts().stream()
                    .map(workoutReq -> createWorkout(workoutReq, plan))
                    .collect(Collectors.toList());
            plan.setWorkouts(workouts);
        }

        return exercisePlanRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public List<ExercisePlan> getPlansByCoach(Long coachId) {
        return exercisePlanRepository.findByCoachId(coachId);
    }

    @Transactional(readOnly = true)
    public List<ExercisePlan> getPlansByTrainee(Long traineeId) {
        return exercisePlanRepository.findByTraineesId(traineeId);
    }

    @Transactional
    public ExercisePlan assignPlanToTrainees(Long planId, Long coachId, List<Long> traineeIds) {
        ExercisePlan plan = exercisePlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise plan not found"));

        if (!plan.getCoach().getId().equals(coachId)) {
            throw new IllegalArgumentException("You do not have permission to modify this plan");
        }

        List<Trainee> newTrainees = traineeRepository.findAllById(traineeIds);
        if (newTrainees.size() != traineeIds.size()) {
            throw new ResourceNotFoundException("One or more trainees not found");
        }

        for (Trainee t : newTrainees) {
            if (!plan.getTrainees().contains(t)) {
                plan.getTrainees().add(t);
            }
        }

        return exercisePlanRepository.save(plan);
    }

    private Workout createWorkout(CreateWorkoutRequest request, ExercisePlan plan) {
        Workout workout = Workout.builder()
                .name(request.getName())
                .notes(request.getNotes())
                .exercisePlan(plan)
                .build();

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (WorkoutExerciseItemRequest item : request.getItems()) {
                Exercise exercise = exerciseRepository.findById(item.getExerciseId())
                        .orElseThrow(() -> new ResourceNotFoundException("Exercise not found with id: " + item.getExerciseId()));

                WorkoutExercise we = WorkoutExercise.builder()
                        .workout(workout)
                        .exercise(exercise)
                        .orderIndex(item.getOrder())
                        .sets(item.getSets())
                        .reps(item.getReps())
                        .load(item.getLoad())
                        .rest(item.getRest())
                        .build();

                workout.getWorkoutExercises().add(we);
            }
        } else if (request.getExerciseIds() != null && !request.getExerciseIds().isEmpty()) {
            // Backward-compatible path for old payloads that only send exerciseIds.
            int order = 1;
            for (Long exerciseId : request.getExerciseIds()) {
                Exercise exercise = exerciseRepository.findById(exerciseId)
                        .orElseThrow(() -> new ResourceNotFoundException("Exercise not found with id: " + exerciseId));

                WorkoutExercise we = WorkoutExercise.builder()
                        .workout(workout)
                        .exercise(exercise)
                        .orderIndex(order++)
                        .sets(3)
                        .reps("10-12")
                        .load(null)
                        .rest("60s")
                        .build();

                workout.getWorkoutExercises().add(we);
            }
        } else {
            throw new IllegalArgumentException("Each workout must include at least one exercise item");
        }

        return workout;
    }
}
