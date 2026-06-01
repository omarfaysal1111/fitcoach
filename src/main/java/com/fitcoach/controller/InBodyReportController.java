package com.fitcoach.controller;

import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.InBodyReportResponse;
import com.fitcoach.service.InBodyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coaches")
@RequiredArgsConstructor
public class InBodyReportController {

    private final InBodyReportService inBodyReportService;

    /**
     * GET /api/coaches/trainees/{id}/inbody-reports
     * List all InBody reports for a trainee (coach must own trainee).
     */
    @GetMapping("/trainees/{id}/inbody-reports")
    public ResponseEntity<ApiResponse<List<InBodyReportResponse>>> getReports(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(inBodyReportService.getReports(principal.getUsername(), id)));
    }

    /**
     * DELETE /api/coaches/inbody-reports/{id}
     * Deletes the DB record and removes the file from disk storage.
     */
    @DeleteMapping("/inbody-reports/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReport(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        inBodyReportService.deleteReport(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("InBody report deleted", null));
    }
}
