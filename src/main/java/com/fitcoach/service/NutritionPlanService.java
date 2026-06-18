package com.fitcoach.service;

import com.fitcoach.domain.entity.*;
import com.fitcoach.domain.enums.NotificationType;
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
    private final FirebaseNotificationService notificationService;
    private final NotificationService appNotificationService;

    @Transactional
    public NutritionPlan createNutritionPlan(Long coachId, CreateNutritionPlanRequest request) {
        Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));

        List<Long> traineeIds = request.getTraineeIds() != null ? request.getTraineeIds() : List.of();
        List<Trainee> trainees = traineeRepository.findAllById(traineeIds);

        NutritionPlan plan = NutritionPlan.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .coach(coach)
                .trainees(trainees)
                .waterTargetLiters(request.getWaterTargetLiters())
                .build();

        if (request.getMeals() != null) {
            for (CreateMealRequest mealRequest : request.getMeals()) {
                Set<Ingredient> ingredients = new HashSet<>();
                double totalCalories = 0.0;
                double totalProtein = 0.0;
                double totalCarbs = 0.0;
                double totalFat = 0.0;

                if (mealRequest.getIngredientIds() != null && !mealRequest.getIngredientIds().isEmpty()) {
                    List<Ingredient> foundIngredients = ingredientRepository.findAllById(mealRequest.getIngredientIds());
                    ingredients.addAll(foundIngredients);
                    totalCalories = foundIngredients.stream().mapToDouble(i -> i.getCalories() != null ? i.getCalories() : 0.0).sum();
                    totalProtein  = foundIngredients.stream().mapToDouble(i -> i.getProtein() != null ? i.getProtein() : 0.0).sum();
                    totalCarbs    = foundIngredients.stream().mapToDouble(i -> i.getCarbohydrates() != null ? i.getCarbohydrates() : 0.0).sum();
                    totalFat      = foundIngredients.stream().mapToDouble(i -> i.getFat() != null ? i.getFat() : 0.0).sum();
                }

                // Coach-provided custom values override ingredient-derived totals
                if (mealRequest.getCustomCalories() != null) totalCalories = mealRequest.getCustomCalories();
                if (mealRequest.getCustomProtein()  != null) totalProtein  = mealRequest.getCustomProtein();
                if (mealRequest.getCustomCarbs()    != null) totalCarbs    = mealRequest.getCustomCarbs();
                if (mealRequest.getCustomFat()      != null) totalFat      = mealRequest.getCustomFat();

                Meal meal = Meal.builder()
                        .name(mealRequest.getName())
                        .calories(totalCalories)
                        .protein(totalProtein)
                        .carbs(totalCarbs)
                        .fat(totalFat)
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

        NutritionPlan saved = nutritionPlanRepository.save(plan);

        // Notify newly assigned trainees (persisted inbox + push)
        for (Trainee t : newTrainees) {
            appNotificationService.notify(
                    t.getUser(),
                    NotificationType.NUTRITION,
                    "New Nutrition Plan Assigned!",
                    "Your coach assigned you a new nutrition plan: " + plan.getTitle()
            );
        }

        return saved;
    }
}
