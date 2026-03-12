package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.dto.request.UpdateCoachRequest;
import com.fitcoach.dto.response.CoachHomeResponse;
import com.fitcoach.dto.response.CoachProfileResponse;
import com.fitcoach.dto.response.InvitationResponse;
import com.fitcoach.dto.response.TraineeProfileResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.TraineeRepository;
import com.fitcoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachService {

    private final CoachRepository coachRepository;
    private final TraineeRepository traineeRepository;
    private final UserRepository userRepository;
    private final InvitationService invitationService;

    @Transactional(readOnly = true)
    public CoachProfileResponse getMyProfile(String email) {
        Coach coach = findCoachByEmail(email);
        return toResponse(coach);
    }

    /**
     * Aggregated view used by the Coach Home screen in the mobile app.
     * Combines the coach profile, all trainees, and all invitations
     * into a single payload.
     */
    @Transactional(readOnly = true)
    public CoachHomeResponse getHome(String email) {
        Coach coach = findCoachByEmail(email);

        List<TraineeProfileResponse> trainees = traineeRepository.findAllByCoachId(coach.getId())
                .stream()
                .map(t -> TraineeProfileResponse.builder()
                        .id(t.getId())
                        .userId(t.getUser().getId())
                        .fullName(t.getUser().getFullName())
                        .email(t.getUser().getEmail())
                        .fitnessGoal(t.getFitnessGoal())
                        .coachId(coach.getId())
                        .coachName(coach.getUser().getFullName())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<InvitationResponse> invitations = invitationService.getInvitationsForCoach(email);

        return CoachHomeResponse.builder()
                .coach(toResponse(coach))
                .trainees(trainees)
                .invitations(invitations)
                .build();
    }

    @Transactional
    public CoachProfileResponse updateMyProfile(String email, UpdateCoachRequest request) {
        Coach coach = findCoachByEmail(email);

        if (StringUtils.hasText(request.getFullName())) {
            coach.getUser().setFullName(request.getFullName());
        }
        if (request.getBio() != null) {
            coach.setBio(request.getBio());
        }
        if (StringUtils.hasText(request.getSpecialisation())) {
            coach.setSpecialisation(request.getSpecialisation());
        }

        coachRepository.save(coach);
        return toResponse(coach);
    }

    @Transactional(readOnly = true)
    public List<TraineeProfileResponse> getMyTrainees(String email) {
        Coach coach = findCoachByEmail(email);
        return traineeRepository.findAllByCoachId(coach.getId())
                .stream()
                .map(t -> TraineeProfileResponse.builder()
                        .id(t.getId())
                        .userId(t.getUser().getId())
                        .fullName(t.getUser().getFullName())
                        .email(t.getUser().getEmail())
                        .fitnessGoal(t.getFitnessGoal())
                        .coachId(coach.getId())
                        .coachName(coach.getUser().getFullName())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Coach findCoachByEmail(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach profile not found"));
    }

    private CoachProfileResponse toResponse(Coach coach) {
        return CoachProfileResponse.builder()
                .id(coach.getId())
                .userId(coach.getUser().getId())
                .fullName(coach.getUser().getFullName())
                .email(coach.getUser().getEmail())
                .specialisation(coach.getSpecialisation())
                .bio(coach.getBio())
                .traineeCount(coach.getTrainees().size())
                .createdAt(coach.getCreatedAt())
                .build();
    }
}
