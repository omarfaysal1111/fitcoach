package com.fitcoach.repository;

import com.fitcoach.domain.entity.TraineeExerciseSetLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TraineeExerciseSetLogRepository extends JpaRepository<TraineeExerciseSetLog, Long> {

    /**
     * Must run before deleting {@link com.fitcoach.domain.entity.TraineeExerciseLog} rows when the DB
     * FK does not use ON DELETE CASCADE (common with Hibernate ddl-auto).
     */
    @Modifying
    @Query("DELETE FROM TraineeExerciseSetLog sl WHERE sl.exerciseLog.workoutCompletion.id = :completionId")
    void deleteAllByWorkoutCompletionId(@Param("completionId") Long completionId);
}
