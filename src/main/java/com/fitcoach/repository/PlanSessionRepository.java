package com.fitcoach.repository;

import com.fitcoach.domain.entity.PlanSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Level 3: one day/session inside a {@link com.fitcoach.domain.entity.WorkoutPlan}
 * (architecture doc "Workout" / daily session).
 */
@Repository
public interface PlanSessionRepository extends JpaRepository<PlanSession, UUID> {

    List<PlanSession> findByPlan_IdOrderByDayOrderAsc(UUID planId);

    Optional<PlanSession> findByIdAndPlan_Id(UUID id, UUID planId);

    boolean existsByIdAndPlan_Id(UUID id, UUID planId);
}
