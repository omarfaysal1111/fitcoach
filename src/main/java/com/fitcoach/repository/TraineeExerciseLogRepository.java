package com.fitcoach.repository;

import com.fitcoach.domain.entity.TraineeExerciseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TraineeExerciseLogRepository extends JpaRepository<TraineeExerciseLog, Long> {

    List<TraineeExerciseLog> findByWorkoutCompletionId(Long workoutCompletionId);

    List<TraineeExerciseLog> findByWorkoutCompletion_Trainee_Id(Long traineeId);

    @Modifying
    @Query("DELETE FROM TraineeExerciseLog l WHERE l.workoutCompletion.id = :completionId")
    void deleteAllByWorkoutCompletionId(@Param("completionId") Long completionId);

    @Query("""
            SELECT DISTINCT l FROM TraineeExerciseLog l
            JOIN FETCH l.workoutCompletion wc
            JOIN FETCH wc.planSession ps
            JOIN FETCH ps.plan
            LEFT JOIN FETCH l.setLogs
            JOIN FETCH l.planSessionExercise pse
            JOIN FETCH pse.exercise
            WHERE wc.trainee.id = :traineeId
            """)
    List<TraineeExerciseLog> findAllByTraineeIdWithAssociations(@Param("traineeId") Long traineeId);
}
