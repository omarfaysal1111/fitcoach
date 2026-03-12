package com.fitcoach.service;

import com.fitcoach.domain.entity.*;
import com.fitcoach.dto.request.CreateExercisePlanRequest;
import com.fitcoach.dto.request.CreateWorkoutRequest;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.ExercisePlanRepository;
import com.fitcoach.repository.ExerciseRepository;
import com.fitcoach.repository.TraineeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        Set<Exercise> exercises = new HashSet<>();
        
        if (request.getExerciseIds() != null && !request.getExerciseIds().isEmpty()) {
            List<Exercise> retrievedExercises = exerciseRepository.findAllById(request.getExerciseIds());
            if (retrievedExercises.size() != request.getExerciseIds().size()) {
                 throw new ResourceNotFoundException("One or more exercises not found");
            }
            exercises.addAll(retrievedExercises);
        }

        return Workout.builder()
                .name(request.getName())
                .notes(request.getNotes())
                .exercises(exercises)
                .exercisePlan(plan)
                .build();
    }
}
