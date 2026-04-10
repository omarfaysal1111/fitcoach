package com.fitcoach.repository;

import com.fitcoach.domain.entity.PlanAssignment;
import com.fitcoach.domain.entity.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Level 2: links a {@link com.fitcoach.domain.entity.WorkoutPlan} to a {@link com.fitcoach.domain.entity.Trainee}.
 */
@Repository
public interface PlanAssignmentRepository extends JpaRepository<PlanAssignment, UUID> {

    List<PlanAssignment> findByTrainee_Id(Long traineeId);

    List<PlanAssignment> findByPlan_Id(UUID planId);

    List<PlanAssignment> findByTrainee_IdAndStatus(Long traineeId, PlanStatus status);

    Optional<PlanAssignment> findByPlan_IdAndTrainee_Id(UUID planId, Long traineeId);

    boolean existsByPlan_IdAndTrainee_Id(UUID planId, Long traineeId);
}
