package com.fitcoach.repository;

import com.fitcoach.domain.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    List<PaymentRecord> findAllByCoachIdOrderByCreatedAtDesc(Long coachId);
}
