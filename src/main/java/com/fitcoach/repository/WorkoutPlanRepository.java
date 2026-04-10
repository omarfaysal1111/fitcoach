package com.fitcoach.repository;

import com.fitcoach.domain.entity.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Level 1: coach-authored {@link WorkoutPlan} (macro template).
 */
@Repository
public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {

    List<WorkoutPlan> findByCoach_Id(Long coachId);

    List<WorkoutPlan> findByCoach_IdOrderByCreatedAtDesc(Long coachId);

    Optional<WorkoutPlan> findByIdAndCoach_Id(UUID id, Long coachId);
}
