package com.fitcoach.repository;

import com.fitcoach.domain.entity.TraineeWorkoutCompletion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TraineeWorkoutCompletionRepository extends JpaRepository<TraineeWorkoutCompletion, Long> {

    Optional<TraineeWorkoutCompletion> findByTrainee_IdAndPlanSession_IdAndCompletionDate(
            Long traineeId, UUID planSessionId, LocalDate completionDate);

    List<TraineeWorkoutCompletion> findByTrainee_IdAndPlanSession_Plan_IdAndCompletionDate(
            Long traineeId, UUID workoutPlanId, LocalDate completionDate);

    List<TraineeWorkoutCompletion> findByTrainee_IdAndCompletionDate(Long traineeId, LocalDate completionDate);

    List<TraineeWorkoutCompletion> findByTrainee_IdAndCompletionDateBetween(
            Long traineeId, LocalDate startDate, LocalDate endDate);

    @EntityGraph(attributePaths = {"planSession", "planSession.plan"})
    List<TraineeWorkoutCompletion> findByTrainee_IdOrderByCompletionDateDescCompletedAtDesc(Long traineeId);
}
