package com.fitcoach.repository;

import com.fitcoach.domain.entity.Invitation;
import com.fitcoach.domain.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByToken(UUID token);

    List<Invitation> findAllByCoachId(Long coachId);

    List<Invitation> findAllByCoachIdAndStatus(Long coachId, InvitationStatus status);

    boolean existsByInviteeEmailAndCoachIdAndStatus(String email, Long coachId, InvitationStatus status);
}
