package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.InBodyReport;
import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.domain.enums.FileType;
import com.fitcoach.dto.response.InBodyReportResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.InBodyReportRepository;
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
public class InBodyReportService {

    private final InBodyReportRepository inBodyReportRepository;
    private final TraineeRepository traineeRepository;
    private final CoachRepository coachRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<InBodyReportResponse> getReports(String coachEmail, Long traineeId) {
        verifyCoachOwnsTrainee(coachEmail, traineeId);
        return inBodyReportRepository.findByTraineeIdOrderByReportDateDesc(traineeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public InBodyReportResponse uploadReport(String coachEmail, Long traineeId,
            MultipartFile file, String label, LocalDate reportDate) {
        Coach coach = findCoachByEmail(coachEmail);
        Trainee trainee = findTraineeOwnedByCoach(traineeId, coach);

        FileType fileType = resolveFileType(file);
        String fileUrl = fileStorageService.store(file, "inbody/" + traineeId);

        InBodyReport report = InBodyReport.builder()
                .trainee(trainee)
                .label(label)
                .fileType(fileType)
                .fileUrl(fileUrl)
                .reportDate(reportDate != null ? reportDate : LocalDate.now())
                .build();

        return toResponse(inBodyReportRepository.save(report));
    }

    @Transactional
    public void deleteReport(String coachEmail, Long reportId) {
        Coach coach = findCoachByEmail(coachEmail);
        InBodyReport report = inBodyReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("InBody report not found"));

        // Ownership: the report's trainee must belong to this coach.
        if (!report.getTrainee().getCoach().getId().equals(coach.getId())) {
            throw new IllegalArgumentException("Not authorized to delete this report");
        }

        fileStorageService.delete(report.getFileUrl());
        inBodyReportRepository.delete(report);
    }

    /** Used internally by CoachService to populate the detail view without an ownership check. */
    public List<InBodyReportResponse> getReportsForTrainee(Long traineeId) {
        return inBodyReportRepository.findByTraineeIdOrderByReportDateDesc(traineeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Trainee uploads their own InBody report. */
    @Transactional
    public InBodyReportResponse uploadReportByTrainee(String traineeEmail,
            MultipartFile file, String label, LocalDate reportDate) {
        var user = userRepository.findByEmail(traineeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Trainee trainee = traineeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainee profile not found"));

        FileType fileType = resolveFileType(file);
        String fileUrl = fileStorageService.store(file, "inbody/" + trainee.getId());

        InBodyReport report = InBodyReport.builder()
                .trainee(trainee)
                .label(label)
                .fileType(fileType)
                .fileUrl(fileUrl)
                .reportDate(reportDate != null ? reportDate : LocalDate.now())
                .build();

        return toResponse(inBodyReportRepository.save(report));
    }

    /** Trainee lists their own InBody reports, newest first. */
    @Transactional(readOnly = true)
    public List<InBodyReportResponse> getMyReports(String traineeEmail) {
        var user = userRepository.findByEmail(traineeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Trainee trainee = traineeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainee profile not found"));
        return inBodyReportRepository.findByTraineeIdOrderByReportDateDesc(trainee.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Trainee deletes one of their own InBody reports. */
    @Transactional
    public void deleteReportByTrainee(String traineeEmail, Long reportId) {
        var user = userRepository.findByEmail(traineeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Trainee trainee = traineeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainee profile not found"));

        InBodyReport report = inBodyReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("InBody report not found"));

        if (!report.getTrainee().getId().equals(trainee.getId())) {
            throw new IllegalArgumentException("Not authorized to delete this report");
        }

        fileStorageService.delete(report.getFileUrl());
        inBodyReportRepository.delete(report);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FileType resolveFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
            return FileType.PDF;
        }
        return FileType.IMAGE;
    }

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
            throw new IllegalArgumentException("Not authorized to manage this trainee's reports");
        }
        return trainee;
    }

    private void verifyCoachOwnsTrainee(String coachEmail, Long traineeId) {
        Coach coach = findCoachByEmail(coachEmail);
        findTraineeOwnedByCoach(traineeId, coach);
    }

    InBodyReportResponse toResponse(InBodyReport report) {
        return InBodyReportResponse.builder()
                .id(report.getId())
                .traineeId(report.getTrainee().getId())
                .label(report.getLabel())
                .fileType(report.getFileType())
                .fileUrl(report.getFileUrl())
                .reportDate(report.getReportDate())
                .uploadedAt(report.getUploadedAt())
                .build();
    }
}
