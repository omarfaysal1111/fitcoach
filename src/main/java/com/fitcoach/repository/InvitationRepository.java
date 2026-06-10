package com.fitcoach.repository;

import com.fitcoach.domain.entity.Invitation;
import com.fitcoach.domain.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByToken(UUID token);

    List<Invitation> findAllByCoachId(Long coachId);

    List<Invitation> findAllByCoachIdAndStatus(Long coachId, InvitationStatus status);

    @Query("SELECT COUNT(i) > 0 FROM Invitation i WHERE LOWER(i.inviteeEmail) = LOWER(:email) AND i.coach.id = :coachId AND i.status = :status")
    boolean existsByInviteeEmailAndCoachIdAndStatus(@Param("email") String email, @Param("coachId") Long coachId, @Param("status") InvitationStatus status);
}
