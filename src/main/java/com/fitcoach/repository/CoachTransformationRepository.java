package com.fitcoach.repository;

import com.fitcoach.domain.entity.CoachTransformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachTransformationRepository extends JpaRepository<CoachTransformation, Long> {
    List<CoachTransformation> findByCoachIdOrderByCreatedAtDesc(Long coachId);
}
