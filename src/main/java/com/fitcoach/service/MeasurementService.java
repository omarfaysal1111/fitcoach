package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.MeasurementLog;
import com.fitcoach.domain.entity.ProgressPicture;
import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.dto.request.CreateMeasurementLogRequest;
import com.fitcoach.dto.request.CreateProgressPictureRequest;
import com.fitcoach.dto.response.MeasurementLogResponse;
import com.fitcoach.dto.response.ProgressPictureResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.MeasurementLogRepository;
import com.fitcoach.repository.ProgressPictureRepository;
import com.fitcoach.repository.TraineeRepository;
import com.fitcoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeasurementService {

    private final MeasurementLogRepository measurementLogRepository;
    private final ProgressPictureRepository progressPictureRepository;
    private final TraineeRepository traineeRepository;
    private final UserRepository userRepository;
    private final CoachRepository coachRepository;

    // ── Trainee: record measurement ──────────────────────────────────────────

    @Transactional
    public MeasurementLogResponse addMeasurement(String email, CreateMeasurementLogRequest request) {
        Trainee trainee = getTraineeByEmail(email);
        MeasurementLog log = measurementLogRepository.save(MeasurementLog.builder()
                .trainee(trainee)
                .date(request.getDate())
                .weight(request.getWeight())
                .bodyFatPercentage(request.getBodyFatPercentage())
                .muscleMass(request.getMuscleMass())
                .waterPercentage(request.getWaterPercentage())
                .chest(request.getChest())
                .waist(request.getWaist())
                .arms(request.getArms())
                .hips(request.getHips())
                .thighs(request.getThighs())
                .recordedAt(LocalDateTime.now())
                .build());
        return toMeasurementResponse(log);
    }

    @Transactional(readOnly = true)
    public List<MeasurementLogResponse> getMyMeasurements(String email) {
        Trainee trainee = getTraineeByEmail(email);
        return measurementLogRepository.findByTraineeIdOrderByDateDesc(trainee.getId())
                .stream().map(this::toMeasurementResponse).collect(Collectors.toList());
    }

    // ── Trainee: record progress picture ────────────────────────────────────

    @Transactional
    public ProgressPictureResponse addProgressPicture(String email, CreateProgressPictureRequest request) {
        Trainee trainee = getTraineeByEmail(email);
        ProgressPicture picture = progressPictureRepository.save(ProgressPicture.builder()
                .trainee(trainee)
                .date(request.getDate())
                .frontPictureUrl(request.getFrontPictureUrl())
                .sidePictureUrl(request.getSidePictureUrl())
                .backPictureUrl(request.getBackPictureUrl())
                .notes(request.getNotes())
                .uploadedAt(LocalDateTime.now())
                .build());
        return toPictureResponse(picture);
    }

    @Transactional(readOnly = true)
    public List<ProgressPictureResponse> getMyProgressPictures(String email) {
        Trainee trainee = getTraineeByEmail(email);
        return progressPictureRepository.findByTraineeIdOrderByDateDesc(trainee.getId())
                .stream().map(this::toPictureResponse).collect(Collectors.toList());
    }

    // ── Coach: view trainee data ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MeasurementLogResponse> getTraineeMeasurements(String coachEmail, Long traineeId) {
        verifyCoachOwnsTrainee(coachEmail, traineeId);
        return measurementLogRepository.findByTraineeIdOrderByDateDesc(traineeId)
                .stream().map(this::toMeasurementResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProgressPictureResponse> getTraineeProgressPictures(String coachEmail, Long traineeId) {
        verifyCoachOwnsTrainee(coachEmail, traineeId);
        return progressPictureRepository.findByTraineeIdOrderByDateDesc(traineeId)
                .stream().map(this::toPictureResponse).collect(Collectors.toList());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Trainee getTraineeByEmail(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return traineeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainee profile not found"));
    }

    private void verifyCoachOwnsTrainee(String coachEmail, Long traineeId) {
        var user = userRepository.findByEmail(coachEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Coach coach = coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));
        boolean owns = coach.getTrainees().stream().anyMatch(t -> t.getId().equals(traineeId));
        if (!owns) {
            throw new ResourceNotFoundException("Trainee not found for this coach");
        }
    }

    private MeasurementLogResponse toMeasurementResponse(MeasurementLog log) {
        return MeasurementLogResponse.builder()
                .id(log.getId())
                .date(log.getDate())
                .weight(log.getWeight())
                .bodyFatPercentage(log.getBodyFatPercentage())
                .muscleMass(log.getMuscleMass())
                .waterPercentage(log.getWaterPercentage())
                .chest(log.getChest())
                .waist(log.getWaist())
                .arms(log.getArms())
                .hips(log.getHips())
                .thighs(log.getThighs())
                .recordedAt(log.getRecordedAt())
                .build();
    }

    private ProgressPictureResponse toPictureResponse(ProgressPicture pic) {
        return ProgressPictureResponse.builder()
                .id(pic.getId())
                .date(pic.getDate())
                .frontPictureUrl(pic.getFrontPictureUrl())
                .sidePictureUrl(pic.getSidePictureUrl())
                .backPictureUrl(pic.getBackPictureUrl())
                .notes(pic.getNotes())
                .uploadedAt(pic.getUploadedAt())
                .build();
    }
}
