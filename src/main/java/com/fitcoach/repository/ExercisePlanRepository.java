package com.fitcoach.repository;

import com.fitcoach.domain.entity.ExercisePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExercisePlanRepository extends JpaRepository<ExercisePlan, Long> {
    List<ExercisePlan> findByCoachId(Long coachId);
    List<ExercisePlan> findByTraineesId(Long traineeId);
}
