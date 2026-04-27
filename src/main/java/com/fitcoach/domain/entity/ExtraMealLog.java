package com.fitcoach.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "extra_meal_logs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtraMealLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainee_id", nullable = false)
    private Trainee trainee;

    /**
     * Display name for this extra meal. Either derived from the resolved ingredient
     * or provided directly as free text by the trainee.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /** Optional reference to an existing ingredient, null when the trainee typed a free-text name. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    /** Calorie estimate provided by the trainee. */
    @Column(nullable = false)
    private int calories;

    /** The date this extra meal was consumed. */
    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @CreatedDate
    @Column(name = "logged_at", updatable = false)
    private LocalDateTime loggedAt;
}
