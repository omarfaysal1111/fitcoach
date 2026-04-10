package com.fitcoach.controller;

import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.CatalogSeedResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.UserRepository;
import com.fitcoach.service.DataSeedingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * On-demand wger catalog seeding (exercises + ingredients). Coach-only.
 */
@RestController
@RequestMapping("/coaches/catalog")
@RequiredArgsConstructor
public class CatalogSeedController {

    private final DataSeedingService dataSeedingService;
    private final UserRepository userRepository;

    /**
     * POST /coaches/catalog/seed?replace=false
     * <p>
     * {@code replace=true} deletes all exercises and ingredients first (requires no FK references).
     */
    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<CatalogSeedResponse>> seedCatalog(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean replace) {

        boolean isCoach = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COACH"));
        if (!isCoach) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only coaches can trigger catalog seeding"));
        }

        userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CatalogSeedResponse result = dataSeedingService.seedCatalog(replace);
        return ResponseEntity.ok(ApiResponse.ok("Catalog seed finished", result));
    }
}
