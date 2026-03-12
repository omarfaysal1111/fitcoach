package com.fitcoach.repository;

import com.fitcoach.domain.entity.NutritionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NutritionPlanRepository extends JpaRepository<NutritionPlan, Long> {
    List<NutritionPlan> findByCoachId(Long coachId);
    List<NutritionPlan> findByTraineesId(Long traineeId);
}
