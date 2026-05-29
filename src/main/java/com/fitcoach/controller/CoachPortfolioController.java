package com.fitcoach.controller;

import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.CoachCertificateResponse;
import com.fitcoach.dto.response.CoachPortfolioItemResponse;
import com.fitcoach.dto.response.CoachPortfolioResponse;
import com.fitcoach.dto.response.CoachTransformationResponse;
import com.fitcoach.service.CoachPortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/coaches/portfolio")
@RequiredArgsConstructor
public class CoachPortfolioController {

    private final CoachPortfolioService portfolioService;

    /** GET /api/coaches/portfolio – authenticated coach's own portfolio */
    @GetMapping
    public ResponseEntity<ApiResponse<CoachPortfolioResponse>> getMyPortfolio(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getMyPortfolio(principal.getUsername())));
    }

    // ── Certificates ──────────────────────────────────────────────────────────

    /** POST /api/coaches/portfolio/certificates – add a certificate (multipart or JSON) */
    @PostMapping(value = "/certificates", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<ApiResponse<CoachCertificateResponse>> addCertificateMultipart(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("title") String title,
            @RequestParam(value = "issuingOrganization", required = false) String issuingOrganization,
            @RequestParam(value = "issueYear", required = false) Integer issueYear,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        CoachCertificateResponse response = portfolioService.addCertificate(
                principal.getUsername(), title, issuingOrganization, issueYear, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Certificate added", response));
    }

    /** POST /api/coaches/portfolio/certificates – JSON variant (no image) */
    @PostMapping(value = "/certificates", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CoachCertificateResponse>> addCertificateJson(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody java.util.Map<String, Object> body) {
        String title = (String) body.get("title");
        String issuingOrganization = (String) body.get("issuingOrganization");
        Object issueYearRaw = body.get("issueYear");
        Integer issueYear = issueYearRaw == null ? null
                : issueYearRaw instanceof Number ? ((Number) issueYearRaw).intValue()
                : Integer.parseInt(issueYearRaw.toString());
        CoachCertificateResponse response = portfolioService.addCertificate(
                principal.getUsername(), title, issuingOrganization, issueYear, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Certificate added", response));
    }

    /** DELETE /api/coaches/portfolio/certificates/{id} */
    @DeleteMapping("/certificates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCertificate(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        portfolioService.deleteCertificate(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Certificate deleted", null));
    }

    // ── Transformations ───────────────────────────────────────────────────────

    /** POST /api/coaches/portfolio/transformations – add a transformation (multipart or JSON) */
    @PostMapping(value = "/transformations", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<ApiResponse<CoachTransformationResponse>> addTransformationMultipart(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("clientLabel") String clientLabel,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "statsSummary", required = false) String statsSummary,
            @RequestParam(value = "beforePhoto", required = false) MultipartFile beforePhoto,
            @RequestParam(value = "afterPhoto", required = false) MultipartFile afterPhoto) {
        CoachTransformationResponse response = portfolioService.addTransformation(
                principal.getUsername(), clientLabel, description, statsSummary, beforePhoto, afterPhoto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Transformation added", response));
    }

    /** POST /api/coaches/portfolio/transformations – JSON variant (no photos) */
    @PostMapping(value = "/transformations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CoachTransformationResponse>> addTransformationJson(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody java.util.Map<String, String> body) {
        CoachTransformationResponse response = portfolioService.addTransformation(
                principal.getUsername(),
                body.get("clientLabel"),
                body.get("description"),
                body.get("statsSummary"),
                null, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Transformation added", response));
    }

    /** DELETE /api/coaches/portfolio/transformations/{id} */
    @DeleteMapping("/transformations/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransformation(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        portfolioService.deleteTransformation(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Transformation deleted", null));
    }

    // ── Portfolio Items ────────────────────────────────────────────────────────

    /** POST /api/coaches/portfolio/items – add an item (multipart or JSON) */
    @PostMapping(value = "/items", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<ApiResponse<CoachPortfolioItemResponse>> addItemMultipart(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        CoachPortfolioItemResponse response = portfolioService.addPortfolioItem(
                principal.getUsername(), title, description, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Portfolio item added", response));
    }

    /** POST /api/coaches/portfolio/items – JSON variant (no image) */
    @PostMapping(value = "/items", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CoachPortfolioItemResponse>> addItemJson(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody java.util.Map<String, String> body) {
        CoachPortfolioItemResponse response = portfolioService.addPortfolioItem(
                principal.getUsername(), body.get("title"), body.get("description"), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Portfolio item added", response));
    }

    /** DELETE /api/coaches/portfolio/items/{id} */
    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        portfolioService.deletePortfolioItem(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Portfolio item deleted", null));
    }
}
