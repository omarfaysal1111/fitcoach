package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.CoachGoal;
import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.dto.request.CoachGoalRequest;
import com.fitcoach.dto.response.CoachGoalResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachGoalRepository;
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
public class CoachGoalService {

    private final CoachGoalRepository coachGoalRepository;
    private final CoachRepository coachRepository;
    private final TraineeRepository traineeRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CoachGoalResponse> getGoals(String coachEmail, Long traineeId) {
        Coach coach = findCoachByEmail(coachEmail);
        Trainee trainee = findTraineeOwnedByCoach(traineeId, coach);
        return coachGoalRepository.findByTraineeIdOrderByCreatedAtDesc(trainee.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CoachGoalResponse createGoal(String coachEmail, Long traineeId, CoachGoalRequest request) {
        Coach coach = findCoachByEmail(coachEmail);
        Trainee trainee = findTraineeOwnedByCoach(traineeId, coach);

        CoachGoal goal = CoachGoal.builder()
                .trainee(trainee)
                .coach(coach)
                .title(request.getTitle())
                .description(request.getDescription())
                .targetDate(request.getTargetDate())
                .build();

        if (request.getStatus() != null) {
            goal.setStatus(request.getStatus());
        }

        return toResponse(coachGoalRepository.save(goal));
    }

    @Transactional
    public CoachGoalResponse updateGoal(String coachEmail, Long goalId, CoachGoalRequest request) {
        Coach coach = findCoachByEmail(coachEmail);
        CoachGoal goal = coachGoalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        if (!goal.getCoach().getId().equals(coach.getId())) {
            throw new IllegalArgumentException("Not authorized to update this goal");
        }

        if (StringUtils.hasText(request.getTitle())) {
            goal.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            goal.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            goal.setStatus(request.getStatus());
        }
        if (request.getTargetDate() != null) {
            goal.setTargetDate(request.getTargetDate());
        }

        return toResponse(coachGoalRepository.save(goal));
    }

    @Transactional
    public void deleteGoal(String coachEmail, Long goalId) {
        Coach coach = findCoachByEmail(coachEmail);
        CoachGoal goal = coachGoalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        if (!goal.getCoach().getId().equals(coach.getId())) {
            throw new IllegalArgumentException("Not authorized to delete this goal");
        }

        coachGoalRepository.delete(goal);
    }

    public List<CoachGoalResponse> getGoalsForTrainee(Long traineeId) {
        return coachGoalRepository.findByTraineeIdOrderByCreatedAtDesc(traineeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Coach findCoachByEmail(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach profile not found"));
    }

    private Trainee findTraineeOwnedByCoach(Long traineeId, Coach coach) {
        Trainee trainee = traineeRepository.findById(traineeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainee not found"));
        if (!trainee.getCoach().getId().equals(coach.getId())) {
            throw new IllegalArgumentException("Not authorized to manage this trainee's goals");
        }
        return trainee;
    }

    CoachGoalResponse toResponse(CoachGoal goal) {
        return CoachGoalResponse.builder()
                .id(goal.getId())
                .traineeId(goal.getTrainee().getId())
                .coachId(goal.getCoach().getId())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .status(goal.getStatus())
                .targetDate(goal.getTargetDate())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
