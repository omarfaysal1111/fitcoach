package com.fitcoach.repository;

import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.domain.enums.TraineeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TraineeRepository extends JpaRepository<Trainee, Long> {
    Optional<Trainee> findByUserId(Long userId);
    List<Trainee> findAllByCoachId(Long coachId);
    boolean existsByUserId(Long userId);
    long countByCoachIdAndStatus(Long coachId, TraineeStatus status);
}
