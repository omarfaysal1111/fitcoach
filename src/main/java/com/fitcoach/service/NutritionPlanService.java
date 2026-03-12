package com.fitcoach.service;

import com.fitcoach.domain.entity.*;
import com.fitcoach.dto.request.CreateMealRequest;
import com.fitcoach.dto.request.CreateNutritionPlanRequest;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NutritionPlanService {

    private final NutritionPlanRepository nutritionPlanRepository;
    private final CoachRepository coachRepository;
    private final TraineeRepository traineeRepository;
    private final IngredientRepository ingredientRepository;

    @Transactional
    public NutritionPlan createNutritionPlan(Long coachId, CreateNutritionPlanRequest request) {
        Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));

        List<Trainee> trainees = traineeRepository.findAllById(request.getTraineeIds());

        NutritionPlan plan = NutritionPlan.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .coach(coach)
                .trainees(trainees)
                .build();

        if (request.getMeals() != null) {
            for (CreateMealRequest mealRequest : request.getMeals()) {
                Set<Ingredient> ingredients = new HashSet<>();
                double totalCalories = 0.0;
                if (mealRequest.getIngredientIds() != null && !mealRequest.getIngredientIds().isEmpty()) {
                    List<Ingredient> foundIngredients = ingredientRepository.findAllById(mealRequest.getIngredientIds());
                    ingredients.addAll(foundIngredients);
                    totalCalories = foundIngredients.stream().mapToDouble(i -> i.getCalories() != null ? i.getCalories() : 0.0).sum();
                }
                
                if (mealRequest.getCustomCalories() != null) {
                    totalCalories = mealRequest.getCustomCalories();
                }

                Meal meal = Meal.builder()
                        .name(mealRequest.getName())
                        .calories(totalCalories)
                        .ingredients(ingredients)
                        .nutritionPlan(plan)
                        .build();
                plan.getMeals().add(meal);
            }
        }

        return nutritionPlanRepository.save(plan);
    }
    
    public List<NutritionPlan> getPlansByCoach(Long coachId) {
        return nutritionPlanRepository.findByCoachId(coachId);
    }
    
    @Transactional(readOnly = true)
    public List<NutritionPlan> getPlansByTrainee(Long traineeId) {
        return nutritionPlanRepository.findByTraineesId(traineeId);
    }

    @Transactional
    public NutritionPlan assignPlanToTrainees(Long planId, Long coachId, List<Long> traineeIds) {
        NutritionPlan plan = nutritionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Nutrition plan not found"));

        if (!plan.getCoach().getId().equals(coachId)) {
            throw new IllegalArgumentException("You do not have permission to modify this plan");
        }

        List<Trainee> newTrainees = traineeRepository.findAllById(traineeIds);
        if (newTrainees.size() != traineeIds.size()) {
            throw new ResourceNotFoundException("One or more trainees not found");
        }

        for (Trainee t : newTrainees) {
            if (!plan.getTrainees().contains(t)) {
                plan.getTrainees().add(t);
            }
        }

        return nutritionPlanRepository.save(plan);
    }
}
