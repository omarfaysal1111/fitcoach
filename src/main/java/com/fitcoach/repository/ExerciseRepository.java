package com.fitcoach.repository;

import com.fitcoach.domain.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    /**
     * IDs of exercises referenced by any plan/workout FK. These rows must be preserved
     * (updated in place) rather than deleted when the catalog is replaced.
     */
    @Query(value = """
            SELECT exercise_id FROM plan_session_exercises WHERE exercise_id IS NOT NULL
            UNION SELECT exercise_id FROM workout_exercises WHERE exercise_id IS NOT NULL
            UNION SELECT exercise_id FROM workout_exercise_items WHERE exercise_id IS NOT NULL
            """, nativeQuery = true)
    List<Long> findReferencedExerciseIds();
}
