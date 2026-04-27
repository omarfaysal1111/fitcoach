package com.fitcoach.service;

import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.domain.entity.TraineeWaterIntake;
import com.fitcoach.dto.request.WaterIntakeUpsertRequest;
import com.fitcoach.dto.response.WaterIntakeResponse;
import com.fitcoach.repository.TraineeWaterIntakeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WaterIntakeService {

    private final TraineeWaterIntakeRepository traineeWaterIntakeRepository;
    private final TraineeService traineeService;

    @Transactional
    public WaterIntakeResponse upsertForTrainee(String email, WaterIntakeUpsertRequest request) {
        Trainee trainee = traineeService.getTraineeByEmail(email);
        LocalDate day = request.getDate() != null ? request.getDate() : LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        TraineeWaterIntake row = traineeWaterIntakeRepository
                .findByTraineeIdAndIntakeDate(trainee.getId(), day)
                .orElseGet(() -> TraineeWaterIntake.builder()
                        .trainee(trainee)
                        .intakeDate(day)
                        .build());
        row.setLiters(request.getLiters());
        row.setUpdatedAt(now);
        traineeWaterIntakeRepository.save(row);

        return WaterIntakeResponse.builder()
                .date(day)
                .liters(row.getLiters())
                .updatedAt(row.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public WaterIntakeResponse getForTrainee(String email, LocalDate date) {
        Trainee trainee = traineeService.getTraineeByEmail(email);
        LocalDate day = date != null ? date : LocalDate.now();

        return traineeWaterIntakeRepository
                .findByTraineeIdAndIntakeDate(trainee.getId(), day)
                .map(r -> WaterIntakeResponse.builder()
                        .date(r.getIntakeDate())
                        .liters(r.getLiters())
                        .updatedAt(r.getUpdatedAt())
                        .build())
                .orElse(WaterIntakeResponse.builder()
                        .date(day)
                        .liters(0.0)
                        .updatedAt(null)
                        .build());
    }
}
