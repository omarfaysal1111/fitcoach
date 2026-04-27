package com.fitcoach.domain.entity;

import com.fitcoach.domain.enums.PaymentMethod;
import com.fitcoach.domain.enums.PaymentStatus;
import com.fitcoach.domain.enums.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_records")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id", nullable = false)
    private Coach coach;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionPlan desiredPlan;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal claimedAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal ocrExtractedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentStatus status;

    @Column(length = 500)
    private String screenshotPath;

    @Column(length = 500)
    private String reviewNote;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
