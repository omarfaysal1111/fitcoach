package com.fitcoach.controller;

import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.ProgressPhotoResponse;
import com.fitcoach.service.ProgressPhotoService;
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
public class ProgressPhotoController {

    private final ProgressPhotoService progressPhotoService;

    /**
     * GET /api/coaches/trainees/{id}/progress-photos
     * List all progress photos for a trainee (coach must own trainee).
     */
    @GetMapping("/trainees/{id}/progress-photos")
    public ResponseEntity<ApiResponse<List<ProgressPhotoResponse>>> getPhotos(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(progressPhotoService.getPhotos(principal.getUsername(), id)));
    }

    /**
     * POST /api/coaches/trainees/{id}/progress-photos
     * Upload a progress photo (image only). The file is stored on disk and only its path is
     * persisted — no image processing is performed.
     *
     * Multipart fields:
     *   file       – required; the image file
     *   label      – optional; e.g. "Front", "Back", "Side", or free notes
     *   photoDate  – optional; ISO date (yyyy-MM-dd); defaults to today
     */
    @PostMapping(value = "/trainees/{id}/progress-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProgressPhotoResponse>> uploadPhoto(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "label", required = false) String label,
            @RequestParam(value = "photoDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate photoDate) {
        ProgressPhotoResponse response =
                progressPhotoService.uploadPhoto(principal.getUsername(), id, file, label, photoDate);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Progress photo uploaded", response));
    }

    /**
     * DELETE /api/coaches/progress-photos/{id}
     * Deletes the DB record and removes the image file from disk storage.
     */
    @DeleteMapping("/progress-photos/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        progressPhotoService.deletePhoto(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Progress photo deleted", null));
    }
}
