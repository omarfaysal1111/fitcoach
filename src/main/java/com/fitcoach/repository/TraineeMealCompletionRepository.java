package com.fitcoach.repository;

import com.fitcoach.domain.entity.TraineeMealCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TraineeMealCompletionRepository extends JpaRepository<TraineeMealCompletion, Long> {

    Optional<TraineeMealCompletion> findByTraineeIdAndMealIdAndCompletionDate(
            Long traineeId, Long mealId, LocalDate completionDate);

    List<TraineeMealCompletion> findByTraineeIdAndMealNutritionPlanIdAndCompletionDate(
            Long traineeId, Long nutritionPlanId, LocalDate completionDate);

    List<TraineeMealCompletion> findByTraineeIdAndCompletionDate(
            Long traineeId, LocalDate completionDate);

    List<TraineeMealCompletion> findByTraineeIdAndCompletionDateBetween(
            Long traineeId, LocalDate startDate, LocalDate endDate);
}

