package com.fitcoach.repository;

import com.fitcoach.domain.entity.PlanSessionExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Level 4: prescribed exercise line on a {@link com.fitcoach.domain.entity.PlanSession}
 * (architecture doc "WorkoutExercise"), referencing catalog {@link com.fitcoach.domain.entity.Exercise}.
 */
@Repository
public interface PlanSessionExerciseRepository extends JpaRepository<PlanSessionExercise, UUID> {

    List<PlanSessionExercise> findByPlanSession_IdOrderByOrderIndexAsc(UUID planSessionId);

    void deleteByPlanSession_Id(UUID planSessionId);
}
