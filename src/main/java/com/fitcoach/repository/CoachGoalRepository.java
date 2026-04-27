package com.fitcoach.repository;

import com.fitcoach.domain.entity.CoachGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachGoalRepository extends JpaRepository<CoachGoal, Long> {
    List<CoachGoal> findByTraineeIdOrderByCreatedAtDesc(Long traineeId);
}
