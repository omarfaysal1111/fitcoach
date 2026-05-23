package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.PaymentRecord;
import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.domain.enums.PaymentStatus;
import com.fitcoach.domain.enums.TraineeStatus;
import com.fitcoach.dto.response.admin.AdminCoachDetailResponse;
import com.fitcoach.dto.response.admin.AdminCoachSummaryResponse;
import com.fitcoach.dto.response.admin.AdminPaymentResponse;
import com.fitcoach.dto.response.admin.AdminStatsResponse;
import com.fitcoach.dto.response.admin.AdminTraineeResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.PaymentRecordRepository;
import com.fitcoach.repository.TraineeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final CoachRepository coachRepository;
    private final TraineeRepository traineeRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    // ─────────────────────────────── Stats ───────────────────────────────────

    /**
     * GET /admin/stats
     * Platform-wide aggregated numbers for the admin dashboard home.
     */
    public AdminStatsResponse getStats() {
        long totalCoaches   = coachRepository.count();
        long totalTrainees  = traineeRepository.count();
        long activeTrainees = traineeRepository.countByStatus(TraineeStatus.ACTIVE);
        long totalPayments  = paymentRecordRepository.count();
        long pendingPayments  = paymentRecordRepository.countByStatus(PaymentStatus.PENDING);
        long approvedPayments = paymentRecordRepository.countByStatus(PaymentStatus.APPROVED);
        long rejectedPayments = paymentRecordRepository.countByStatus(PaymentStatus.REJECTED);

        return AdminStatsResponse.builder()
                .totalCoaches(totalCoaches)
                .totalTrainees(totalTrainees)
                .activeTrainees(activeTrainees)
                .totalPayments(totalPayments)
                .pendingPayments(pendingPayments)
                .approvedPayments(approvedPayments)
                .rejectedPayments(rejectedPayments)
                .totalApprovedRevenue(
                        paymentRecordRepository.sumClaimedAmountByStatus(PaymentStatus.APPROVED))
                .build();
    }

    // ─────────────────────────────── Coaches ─────────────────────────────────

    /**
     * GET /admin/coaches
     * Summary list of every coach with their trainee counts.
     */
    public List<AdminCoachSummaryResponse> getAllCoaches() {
        return coachRepository.findAll()
                .stream()
                .map(this::toCoachSummary)
                .toList();
    }

    /**
     * GET /admin/coaches/{coachId}
     * Full profile of a single coach including their complete trainee roster.
     */
    public AdminCoachDetailResponse getCoachDetail(Long coachId) {
        Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found with id: " + coachId));

        List<Trainee> trainees = traineeRepository.findAllByCoachId(coachId);
        long activeCount = trainees.stream()
                .filter(t -> TraineeStatus.ACTIVE.equals(t.getStatus()))
                .count();

        return AdminCoachDetailResponse.builder()
                .coachId(coach.getId())
                .fullName(coach.getUser().getFullName())
                .email(coach.getUser().getEmail())
                .specialisation(coach.getSpecialisation())
                .bio(coach.getBio())
                .totalTrainees(trainees.size())
                .activeTrainees(activeCount)
                .memberSince(coach.getCreatedAt())
                .trainees(trainees.stream().map(this::toTraineeResponse).toList())
                .build();
    }

    // ─────────────────────────────── Trainees ────────────────────────────────

    /**
     * GET /admin/coaches/{coachId}/trainees
     * All trainees that belong to a specific coach.
     */
    public List<AdminTraineeResponse> getTraineesForCoach(Long coachId) {
        if (!coachRepository.existsById(coachId)) {
            throw new ResourceNotFoundException("Coach not found with id: " + coachId);
        }
        return traineeRepository.findAllByCoachId(coachId)
                .stream()
                .map(this::toTraineeResponse)
                .toList();
    }

    // ─────────────────────────────── Payments ────────────────────────────────

    /**
     * GET /admin/payments
     * All payment records across every coach, newest first.
     */
    public List<AdminPaymentResponse> getAllPayments() {
        return paymentRecordRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    /**
     * GET /admin/coaches/{coachId}/payments
     * All payment records submitted by a specific coach, newest first.
     */
    public List<AdminPaymentResponse> getPaymentsForCoach(Long coachId) {
        if (!coachRepository.existsById(coachId)) {
            throw new ResourceNotFoundException("Coach not found with id: " + coachId);
        }
        return paymentRecordRepository.findAllByCoachIdOrderByCreatedAtDesc(coachId)
                .stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    // ─────────────────────────────── Mappers ─────────────────────────────────

    private AdminCoachSummaryResponse toCoachSummary(Coach coach) {
        long total  = traineeRepository.findAllByCoachId(coach.getId()).size();
        long active = traineeRepository.countByCoachIdAndStatus(coach.getId(), TraineeStatus.ACTIVE);

        return AdminCoachSummaryResponse.builder()
                .coachId(coach.getId())
                .fullName(coach.getUser().getFullName())
                .email(coach.getUser().getEmail())
                .specialisation(coach.getSpecialisation())
                .totalTrainees(total)
                .activeTrainees(active)
                .memberSince(coach.getCreatedAt())
                .build();
    }

    private AdminTraineeResponse toTraineeResponse(Trainee trainee) {
        return AdminTraineeResponse.builder()
                .traineeId(trainee.getId())
                .fullName(trainee.getUser().getFullName())
                .email(trainee.getUser().getEmail())
                .status(trainee.getStatus())
                .fitnessGoal(trainee.getFitnessGoal())
                .traineeLevel(trainee.getTraineeLevel())
                .currentStreak(trainee.getCurrentStreak())
                .joinedAt(trainee.getCreatedAt())
                .build();
    }

    private AdminPaymentResponse toPaymentResponse(PaymentRecord record) {
        Coach coach = record.getCoach();
        return AdminPaymentResponse.builder()
                .paymentId(record.getId())
                .coachId(coach.getId())
                .coachName(coach.getUser().getFullName())
                .coachEmail(coach.getUser().getEmail())
                .desiredPlan(record.getDesiredPlan())
                .paymentMethod(record.getPaymentMethod())
                .claimedAmount(record.getClaimedAmount())
                .ocrExtractedAmount(record.getOcrExtractedAmount())
                .status(record.getStatus())
                .reviewNote(record.getReviewNote())
                .screenshotPath(record.getScreenshotPath())
                .submittedAt(record.getCreatedAt())
                .build();
    }
}
