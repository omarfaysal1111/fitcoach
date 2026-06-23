package com.fitcoach.controller;

import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.admin.AdminCoachDetailResponse;
import com.fitcoach.dto.response.admin.AdminCoachSummaryResponse;
import com.fitcoach.dto.response.admin.AdminPaymentResponse;
import com.fitcoach.dto.response.admin.AdminStatsResponse;
import com.fitcoach.dto.response.admin.AdminTraineePlansResponse;
import com.fitcoach.dto.response.admin.AdminTraineeResponse;
import com.fitcoach.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only endpoints – all routes under /admin/** require the ADMIN role.
 *
 * Endpoints
 * ─────────────────────────────────────────────────────────────
 * GET  /admin/stats                          → platform-wide KPIs
 * GET  /admin/coaches                        → all coaches + trainee counts
 * GET  /admin/coaches/{coachId}              → coach detail + trainee roster
 * GET  /admin/coaches/{coachId}/trainees     → trainee list for a coach
 * GET  /admin/trainees/plans?email=          → plans assigned to a trainee (by email)
 * GET  /admin/payments                       → all payments (all coaches)
 * GET  /admin/coaches/{coachId}/payments     → payments for one coach
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ─────────────────────────────── Stats ───────────────────────────────────

    /**
     * GET /admin/stats
     * Returns platform-wide KPIs: total coaches, trainees, payments, and revenue.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getStats()));
    }

    // ─────────────────────────────── Coaches ─────────────────────────────────

    /**
     * GET /admin/coaches
     * Returns a summary list of all coaches with their trainee counts.
     */
    @GetMapping("/coaches")
    public ResponseEntity<ApiResponse<List<AdminCoachSummaryResponse>>> getAllCoaches() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAllCoaches()));
    }

    /**
     * GET /admin/coaches/{coachId}
     * Returns full details of a single coach including their complete trainee roster.
     */
    @GetMapping("/coaches/{coachId}")
    public ResponseEntity<ApiResponse<AdminCoachDetailResponse>> getCoachDetail(
            @PathVariable Long coachId) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getCoachDetail(coachId)));
    }

    // ─────────────────────────────── Trainees ────────────────────────────────

    /**
     * GET /admin/coaches/{coachId}/trainees
     * Returns all trainees that belong to the given coach.
     */
    @GetMapping("/coaches/{coachId}/trainees")
    public ResponseEntity<ApiResponse<List<AdminTraineeResponse>>> getTraineesForCoach(
            @PathVariable Long coachId) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getTraineesForCoach(coachId)));
    }

    /**
     * GET /admin/trainees/plans?email={email}
     * Returns every workout and nutrition plan assigned to the trainee identified
     * by the given email address.
     */
    @GetMapping("/trainees/plans")
    public ResponseEntity<ApiResponse<AdminTraineePlansResponse>> getTraineePlansByEmail(
            @RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getPlansForTraineeByEmail(email)));
    }

    // ─────────────────────────────── Payments ────────────────────────────────

    /**
     * GET /admin/payments
     * Returns all payment records across every coach, sorted newest-first.
     */
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<AdminPaymentResponse>>> getAllPayments() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAllPayments()));
    }

    /**
     * GET /admin/coaches/{coachId}/payments
     * Returns all payment records submitted by a specific coach, sorted newest-first.
     */
    @GetMapping("/coaches/{coachId}/payments")
    public ResponseEntity<ApiResponse<List<AdminPaymentResponse>>> getPaymentsForCoach(
            @PathVariable Long coachId) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getPaymentsForCoach(coachId)));
    }
}
