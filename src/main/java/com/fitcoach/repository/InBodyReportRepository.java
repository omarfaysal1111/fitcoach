package com.fitcoach.repository;

import com.fitcoach.domain.entity.InBodyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InBodyReportRepository extends JpaRepository<InBodyReport, Long> {
    List<InBodyReport> findByTraineeIdOrderByReportDateDesc(Long traineeId);
}
