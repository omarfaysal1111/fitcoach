package com.fitcoach.controller;

import com.fitcoach.domain.entity.Exercise;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseRepository exerciseRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Exercise>>> getAllExercises() {
        List<Exercise> exercises = exerciseRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok("Exercises retrieved successfully", exercises));
    }
}
