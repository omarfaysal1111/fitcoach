package com.fitcoach.repository;

import com.fitcoach.domain.entity.CoachSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoachSubscriptionRepository extends JpaRepository<CoachSubscription, Long> {
    Optional<CoachSubscription> findByCoachId(Long coachId);
}
