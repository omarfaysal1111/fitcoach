package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.Invitation;
import com.fitcoach.domain.enums.InvitationStatus;
import com.fitcoach.dto.request.InvitationRequest;
import com.fitcoach.dto.response.InvitationResponse;
import com.fitcoach.exception.ConflictException;
import com.fitcoach.exception.InvitationException;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.InvitationRepository;
import com.fitcoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final CoachRepository coachRepository;
    private final UserRepository userRepository;

    @Value("${app.invitation.expiry-days}")
    private int expiryDays;

    // ── Create Invitation ─────────────────────────────────────────────────────
    @Transactional
    public InvitationResponse createInvitation(String coachEmail, InvitationRequest request) {
        Coach coach = findCoachByEmail(coachEmail);

        if (invitationRepository.existsByInviteeEmailAndCoachIdAndStatus(
                request.getEmail(), coach.getId(), InvitationStatus.PENDING)) {
            throw new ConflictException(
                    "A pending invitation already exists for: " + request.getEmail());
        }

        Invitation invitation = Invitation.builder()
                .coach(coach)
                .inviteeEmail(request.getEmail())
                .expiresAt(LocalDateTime.now().plusDays(expiryDays))
                .build();

        invitationRepository.save(invitation);
        return toResponse(invitation);
    }

    // ── List Coach Invitations ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<InvitationResponse> getInvitationsForCoach(String coachEmail) {
        Coach coach = findCoachByEmail(coachEmail);
        return invitationRepository.findAllByCoachId(coach.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Consume Invitation (called from AuthService) ───────────────────────────
    @Transactional
    public Coach consumeInvitation(UUID token, String email) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new InvitationException("Invitation not found or invalid"));

        if (!invitation.isValid()) {
            throw new InvitationException("Invitation is expired or has already been used");
        }

        if (!invitation.getInviteeEmail().equalsIgnoreCase(email)) {
            throw new InvitationException("This invitation was not issued for email: " + email);
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        return invitation.getCoach();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Coach findCoachByEmail(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach profile not found"));
    }

    private InvitationResponse toResponse(Invitation invitation) {
        return InvitationResponse.builder()
                .token(invitation.getToken())
                .inviteeEmail(invitation.getInviteeEmail())
                .status(invitation.getStatus())
                .expiresAt(invitation.getExpiresAt())
                .createdAt(invitation.getCreatedAt())
                .build();
    }
}
