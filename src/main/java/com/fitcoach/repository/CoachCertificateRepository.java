package com.fitcoach.repository;

import com.fitcoach.domain.entity.CoachCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachCertificateRepository extends JpaRepository<CoachCertificate, Long> {
    List<CoachCertificate> findByCoachIdOrderByCreatedAtDesc(Long coachId);
}
