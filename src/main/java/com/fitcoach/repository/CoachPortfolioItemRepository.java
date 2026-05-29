package com.fitcoach.repository;

import com.fitcoach.domain.entity.CoachPortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachPortfolioItemRepository extends JpaRepository<CoachPortfolioItem, Long> {
    List<CoachPortfolioItem> findByCoachIdOrderByCreatedAtDesc(Long coachId);
}
