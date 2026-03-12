package com.fitcoach.repository;

import com.fitcoach.domain.entity.TraineeWorkoutCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TraineeWorkoutCompletionRepository extends JpaRepository<TraineeWorkoutCompletion, Long> {

    Optional<TraineeWorkoutCompletion> findByTraineeIdAndWorkoutIdAndCompletionDate(
            Long traineeId, Long workoutId, LocalDate completionDate);

    List<TraineeWorkoutCompletion> findByTraineeIdAndWorkoutExercisePlanIdAndCompletionDate(
            Long traineeId, Long exercisePlanId, LocalDate completionDate);
}

