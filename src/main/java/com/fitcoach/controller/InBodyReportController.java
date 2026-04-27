package com.fitcoach.controller;

import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.InBodyReportResponse;
import com.fitcoach.service.InBodyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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
     * POST /api/coaches/trainees/{id}/inbody-reports
     * Upload a raw InBody file (PDF or image). The file is stored on disk and its path is
     * persisted in the DB. No content parsing or data extraction is performed.
     *
     * Multipart fields:
     *   file        – required; the PDF or image file
     *   label       – optional; human-readable label (e.g. "Week 4 scan")
     *   reportDate  – optional; ISO date (yyyy-MM-dd) of the measurement; defaults to today
     */
    @PostMapping(value = "/trainees/{id}/inbody-reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InBodyReportResponse>> uploadReport(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "label", required = false) String label,
            @RequestParam(value = "reportDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {
        InBodyReportResponse response =
                inBodyReportService.uploadReport(principal.getUsername(), id, file, label, reportDate);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("InBody report uploaded", response));
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
