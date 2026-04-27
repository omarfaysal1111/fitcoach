package com.fitcoach.repository;

import com.fitcoach.domain.entity.MealIngredientDeviation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealIngredientDeviationRepository extends JpaRepository<MealIngredientDeviation, Long> {

    List<MealIngredientDeviation> findByCompletionIdIn(List<Long> completionIds);
}
