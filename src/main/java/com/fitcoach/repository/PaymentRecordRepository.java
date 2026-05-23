package com.fitcoach.repository;

import com.fitcoach.domain.entity.PaymentRecord;
import com.fitcoach.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    List<PaymentRecord> findAllByCoachIdOrderByCreatedAtDesc(Long coachId);

    List<PaymentRecord> findAllByOrderByCreatedAtDesc();

    long countByStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.claimedAmount), 0) FROM PaymentRecord p WHERE p.status = :status")
    BigDecimal sumClaimedAmountByStatus(PaymentStatus status);
}
