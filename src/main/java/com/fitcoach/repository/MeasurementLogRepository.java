package com.fitcoach.repository;

import com.fitcoach.domain.entity.MeasurementLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeasurementLogRepository extends JpaRepository<MeasurementLog, Long> {

    List<MeasurementLog> findByTraineeIdOrderByDateDesc(Long traineeId);
}
