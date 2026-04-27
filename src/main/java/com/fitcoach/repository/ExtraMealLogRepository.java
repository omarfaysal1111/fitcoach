package com.fitcoach.repository;

import com.fitcoach.domain.entity.ExtraMealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExtraMealLogRepository extends JpaRepository<ExtraMealLog, Long> {
    List<ExtraMealLog> findByTraineeIdOrderByMealDateDesc(Long traineeId);
    List<ExtraMealLog> findByTraineeIdAndMealDate(Long traineeId, LocalDate mealDate);
}
