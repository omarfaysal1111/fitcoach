package com.fitcoach.service;

import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.dto.request.UpdateTraineeRequest;
import com.fitcoach.dto.response.CoachProfileResponse;
import com.fitcoach.dto.response.TraineeProfileResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.TraineeRepository;
import com.fitcoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TraineeService {

    private final TraineeRepository traineeRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public TraineeProfileResponse getMyProfile(String email) {
        return toResponse(findTraineeByEmail(email));
    }

    @Transactional
    public TraineeProfileResponse updateMyProfile(String email, UpdateTraineeRequest request) {
        Trainee trainee = findTraineeByEmail(email);

        if (StringUtils.hasText(request.getFullName())) {
            trainee.getUser().setFullName(request.getFullName());
        }
        if (StringUtils.hasText(request.getFitnessGoal())) {
            trainee.setFitnessGoal(request.getFitnessGoal());
        }

        traineeRepository.save(trainee);
        return toResponse(trainee);
    }

    @Transactional(readOnly = true)
    public CoachProfileResponse getMyCoach(String email) {
        Trainee trainee = findTraineeByEmail(email);
        var coach = trainee.getCoach();
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

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Trainee findTraineeByEmail(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return traineeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainee profile not found"));
    }

    private TraineeProfileResponse toResponse(Trainee trainee) {
        var coach = trainee.getCoach();
        return TraineeProfileResponse.builder()
                .id(trainee.getId())
                .userId(trainee.getUser().getId())
                .fullName(trainee.getUser().getFullName())
                .email(trainee.getUser().getEmail())
                .fitnessGoal(trainee.getFitnessGoal())
                .coachId(coach.getId())
                .coachName(coach.getUser().getFullName())
                .createdAt(trainee.getCreatedAt())
                .build();
    }
}
