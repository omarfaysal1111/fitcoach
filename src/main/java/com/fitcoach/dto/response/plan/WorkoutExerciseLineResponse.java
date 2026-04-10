package com.fitcoach.dto.response.plan;

import com.fitcoach.domain.entity.SectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Prescribed exercise on a plan day (API: workout exercise; entity: {@link com.fitcoach.domain.entity.PlanSessionExercise}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutExerciseLineResponse {

    private UUID id;
    private UUID workoutId;
    private Long exerciseId;
    private SectionType sectionType;
    private Integer orderIndex;
    private Integer sets;
    private String reps;
    private BigDecimal loadAmount;
    private Integer restSeconds;
}
