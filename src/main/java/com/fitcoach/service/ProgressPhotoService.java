package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.ProgressPhoto;
import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.dto.response.ProgressPhotoResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.ProgressPhotoRepository;
import com.fitcoach.repository.TraineeRepository;
import com.fitcoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressPhotoService {

    private final ProgressPhotoRepository progressPhotoRepository;
    private final TraineeRepository traineeRepository;
    private final CoachRepository coachRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<ProgressPhotoResponse> getPhotos(String coachEmail, Long traineeId) {
        verifyCoachOwnsTrainee(coachEmail, traineeId);
        return progressPhotoRepository.findByTraineeIdOrderByPhotoDateDesc(traineeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProgressPhotoResponse uploadPhoto(String coachEmail, Long traineeId,
            MultipartFile file, String label, LocalDate photoDate) {
        Coach coach = findCoachByEmail(coachEmail);
        Trainee trainee = findTraineeOwnedByCoach(traineeId, coach);

        String fileUrl = fileStorageService.store(file, "progress-photos/" + traineeId);

        ProgressPhoto photo = ProgressPhoto.builder()
                .trainee(trainee)
                .label(label)
                .fileUrl(fileUrl)
                .photoDate(photoDate != null ? photoDate : LocalDate.now())
                .build();

        return toResponse(progressPhotoRepository.save(photo));
    }

    @Transactional
    public void deletePhoto(String coachEmail, Long photoId) {
        Coach coach = findCoachByEmail(coachEmail);
        ProgressPhoto photo = progressPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Progress photo not found"));

        if (!photo.getTrainee().getCoach().getId().equals(coach.getId())) {
            throw new IllegalArgumentException("Not authorized to delete this photo");
        }

        fileStorageService.delete(photo.getFileUrl());
        progressPhotoRepository.delete(photo);
    }

    /** Used internally by CoachService to populate the detail view without an ownership re-check. */
    public List<ProgressPhotoResponse> getPhotosForTrainee(Long traineeId) {
        return progressPhotoRepository.findByTraineeIdOrderByPhotoDateDesc(traineeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Trainee uploads their own progress photo. */
    @Transactional
    public ProgressPhotoResponse uploadPhotoByTrainee(String traineeEmail,
            MultipartFile file, String label, LocalDate photoDate) {
        var user = userRepository.findByEmail(traineeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Trainee trainee = traineeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainee profile not found"));

        String fileUrl = fileStorageService.store(file, "progress-photos/" + trainee.getId());

        ProgressPhoto photo = ProgressPhoto.builder()
                .trainee(trainee)
                .label(label)
                .fileUrl(fileUrl)
                .photoDate(photoDate != null ? photoDate : LocalDate.now())
                .build();

        return toResponse(progressPhotoRepository.save(photo));
    }

    /** Trainee lists their own progress photos, newest first. */
    @Transactional(readOnly = true)
    public List<ProgressPhotoResponse> getMyPhotos(String traineeEmail) {
        var user = userRepository.findByEmail(traineeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Trainee trainee = traineeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainee profile not found"));
        return progressPhotoRepository.findByTraineeIdOrderByPhotoDateDesc(trainee.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Trainee deletes one of their own progress photos. */
    @Transactional
    public void deletePhotoByTrainee(String traineeEmail, Long photoId) {
        var user = userRepository.findByEmail(traineeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Trainee trainee = traineeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainee profile not found"));

        ProgressPhoto photo = progressPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Progress photo not found"));

        if (!photo.getTrainee().getId().equals(trainee.getId())) {
            throw new IllegalArgumentException("Not authorized to delete this photo");
        }

        fileStorageService.delete(photo.getFileUrl());
        progressPhotoRepository.delete(photo);
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
            throw new IllegalArgumentException("Not authorized to manage this trainee's photos");
        }
        return trainee;
    }

    private void verifyCoachOwnsTrainee(String coachEmail, Long traineeId) {
        Coach coach = findCoachByEmail(coachEmail);
        findTraineeOwnedByCoach(traineeId, coach);
    }

    ProgressPhotoResponse toResponse(ProgressPhoto photo) {
        return ProgressPhotoResponse.builder()
                .id(photo.getId())
                .traineeId(photo.getTrainee().getId())
                .label(photo.getLabel())
                .fileUrl(photo.getFileUrl())
                .photoDate(photo.getPhotoDate())
                .uploadedAt(photo.getUploadedAt())
                .build();
    }
}
