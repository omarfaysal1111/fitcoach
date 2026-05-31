package com.fitcoach.service;

import com.fitcoach.domain.entity.Exercise;
import com.fitcoach.dto.response.ExerciseResponse;
import com.fitcoach.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public List<ExerciseResponse> getAllExercises() {
        return exerciseRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ExerciseResponse toResponse(Exercise exercise) {
        return ExerciseResponse.builder()
                .id(exercise.getId())
                .name(exercise.getName())
                .description(exercise.getDescription())
                .videoLink(resolveVideoUrl(exercise.getVideoLink()))
                .targetedMuscle(exercise.getTargetedMuscle())
                .build();
    }

    private String resolveVideoUrl(String storedLink) {
        if (storedLink == null || storedLink.contains("wger.de")) {
            return null;
        }
        return storedLink;
    }
}
