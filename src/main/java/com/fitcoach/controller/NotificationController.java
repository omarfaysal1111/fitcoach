package com.fitcoach.controller;

import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.NotificationResponse;
import com.fitcoach.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(notificationService.getForUser(principal.getUsername())));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(
            @AuthenticationPrincipal UserDetails principal) {
        long count = notificationService.unreadCount(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        notificationService.markRead(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", null));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal UserDetails principal) {
        notificationService.markAllRead(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", null));
    }
}
