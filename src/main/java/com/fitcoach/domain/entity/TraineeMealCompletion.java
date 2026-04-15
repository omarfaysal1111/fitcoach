package com.fitcoach.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trainee_meal_completions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"trainee_id", "meal_id", "completion_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraineeMealCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "trainee_id")
    private Trainee trainee;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id")
    private Meal meal;

    /**
     * Nullable in some existing DB rows. We treat NULL as false.
     * Keep the column name as-is for backwards compatibility.
     */
    @Column(name = "is_skipped")
    @Builder.Default
    private Boolean skipped = false;

    @Column(name = "completion_date", nullable = false)
    private LocalDate completionDate;

    @Column(nullable = false)
    private LocalDateTime completedAt;

    @PrePersist
    @PreUpdate
    void coalesceNulls() {
        if (skipped == null) {
            skipped = false;
        }
    }

    public boolean isSkipped() {
        return Boolean.TRUE.equals(skipped);
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }
}

