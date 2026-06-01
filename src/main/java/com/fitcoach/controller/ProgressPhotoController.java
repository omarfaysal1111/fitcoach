package com.fitcoach.controller;

import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.ProgressPhotoResponse;
import com.fitcoach.service.ProgressPhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
