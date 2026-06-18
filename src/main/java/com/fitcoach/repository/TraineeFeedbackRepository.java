package com.fitcoach.repository;

import com.fitcoach.domain.entity.TraineeFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TraineeFeedbackRepository extends JpaRepository<TraineeFeedback, Long> {

    /** All feedback a trainee has sent, newest first. */
    List<TraineeFeedback> findByTrainee_IdOrderByCreatedAtDesc(Long traineeId);
}
