package com.fitcoach.repository;

import com.fitcoach.domain.entity.TraineeWaterIntake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TraineeWaterIntakeRepository extends JpaRepository<TraineeWaterIntake, Long> {

    Optional<TraineeWaterIntake> findByTraineeIdAndIntakeDate(Long traineeId, LocalDate intakeDate);

    List<TraineeWaterIntake> findByTraineeIdAndIntakeDateBetween(Long traineeId, LocalDate start, LocalDate end);
}
