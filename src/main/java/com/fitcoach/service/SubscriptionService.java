package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.CoachSubscription;
import com.fitcoach.domain.enums.SubscriptionPlan;
import com.fitcoach.domain.enums.SubscriptionStatus;
import com.fitcoach.domain.enums.TraineeStatus;
import com.fitcoach.dto.response.SubscriptionStatusResponse;
import com.fitcoach.exception.SubscriptionException;
import com.fitcoach.repository.CoachSubscriptionRepository;
import com.fitcoach.repository.TraineeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final CoachSubscriptionRepository subscriptionRepository;
    private final TraineeRepository traineeRepository;

    // ── Trial Init (called on coach registration) ─────────────────────────────

    @Transactional
    public void initTrial(Coach coach) {
        LocalDate now = LocalDate.now();
        CoachSubscription trial = CoachSubscription.builder()
                .coach(coach)
                .plan(SubscriptionPlan.TRIAL)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(now.plusDays(SubscriptionPlan.TRIAL.getDurationDays()))
                .updatedAt(LocalDateTime.now())
                .build();
        subscriptionRepository.save(trial);
        log.info("Trial subscription initialised for coach id={}", coach.getId());
    }

    // ── Activate Plan (called after successful payment) ───────────────────────

    @Transactional
    public CoachSubscription activatePlan(Coach coach, SubscriptionPlan plan) {
        CoachSubscription sub = subscriptionRepository.findByCoachId(coach.getId())
                .orElseGet(() -> CoachSubscription.builder().coach(coach).build());

        LocalDate now = LocalDate.now();
        sub.setPlan(plan);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartDate(now);
        sub.setEndDate(now.plusDays(plan.getDurationDays()));
        sub.setUpdatedAt(LocalDateTime.now());

        CoachSubscription saved = subscriptionRepository.save(sub);
        log.info("Subscription activated: coach id={}, plan={}, until={}", coach.getId(), plan, saved.getEndDate());
        return saved;
    }

    // ── Subscription Status ───────────────────────────────────────────────────

    @Transactional
    public SubscriptionStatusResponse getStatus(Coach coach) {
        CoachSubscription sub = getOrCreateTrial(coach);
        expireIfNeeded(sub);

        int activeCount = countActiveTrainees(coach.getId());
        int max = sub.getPlan().getMaxClients();
        boolean unlimited = sub.getPlan().isUnlimited();

        return SubscriptionStatusResponse.builder()
                .plan(sub.getPlan())
                .status(sub.getStatus())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .active(sub.isActive())
                .maxClients(unlimited ? -1 : max)
                .currentClientCount(activeCount)
                .remainingSlots(unlimited ? -1 : Math.max(0, max - activeCount))
                .unlimited(unlimited)
                .build();
    }

    // ── Limit Guard (called before issuing an invitation) ────────────────────

    @Transactional
    public void assertCanInvite(Coach coach) {
        CoachSubscription sub = getOrCreateTrial(coach);
        expireIfNeeded(sub);

        if (!sub.isActive()) {
            throw new SubscriptionException(
                    "Your subscription has expired. Please renew to invite new trainees.");
        }

        if (!sub.getPlan().isUnlimited()) {
            int activeCount = countActiveTrainees(coach.getId());
            if (activeCount >= sub.getPlan().getMaxClients()) {
                throw new SubscriptionException(String.format(
                        "You have reached the client limit for your %s plan (%d/%d). " +
                        "Please upgrade to invite more trainees.",
                        sub.getPlan().name(),
                        activeCount,
                        sub.getPlan().getMaxClients()));
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CoachSubscription getOrCreateTrial(Coach coach) {
        return subscriptionRepository.findByCoachId(coach.getId())
                .orElseGet(() -> {
                    log.warn("No subscription found for coach id={}; creating trial on-the-fly", coach.getId());
                    CoachSubscription trial = CoachSubscription.builder()
                            .coach(coach)
                            .plan(SubscriptionPlan.TRIAL)
                            .status(SubscriptionStatus.ACTIVE)
                            .startDate(coach.getCreatedAt() != null
                                    ? coach.getCreatedAt().toLocalDate()
                                    : LocalDate.now())
                            .endDate(LocalDate.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return subscriptionRepository.save(trial);
                });
    }

    private void expireIfNeeded(CoachSubscription sub) {
        if (sub.getStatus() == SubscriptionStatus.ACTIVE
                && LocalDate.now().isAfter(sub.getEndDate())) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            sub.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(sub);
        }
    }

    private int countActiveTrainees(Long coachId) {
        return (int) traineeRepository.countByCoachIdAndStatus(coachId, TraineeStatus.ACTIVE);
    }
}
