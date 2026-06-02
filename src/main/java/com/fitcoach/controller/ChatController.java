package com.fitcoach.controller;

import com.fitcoach.domain.entity.User;
import com.fitcoach.dto.request.ChatNotifyRequest;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.TraineeRepository;
import com.fitcoach.repository.UserRepository;
import com.fitcoach.service.FirebaseNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final UserRepository userRepository;
    private final TraineeRepository traineeRepository;
    private final CoachRepository coachRepository;
    private final FirebaseNotificationService notificationService;

    @PostMapping("/notify")
    public ResponseEntity<ApiResponse<Void>> notifyChatMessage(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ChatNotifyRequest request) {

        String senderName = userRepository.findByEmail(principal.getUsername())
                .map(User::getFullName)
                .orElse("New Message");

        String fcmToken = resolveFcmToken(request.getRecipientEntityId(), request.getRecipientRole());

        if (fcmToken != null && !fcmToken.isBlank()) {
            String body = request.getText().length() > 80
                    ? request.getText().substring(0, 77) + "..."
                    : request.getText();
            notificationService.sendChatNotification(fcmToken, senderName, body, request.getConversationId());
        }

        return ResponseEntity.ok(ApiResponse.ok("Notification sent", null));
    }

    private String resolveFcmToken(Long entityId, String role) {
        if ("TRAINEE".equalsIgnoreCase(role)) {
            return traineeRepository.findById(entityId)
                    .map(t -> t.getUser() != null ? t.getUser().getFcmToken() : null)
                    .orElse(null);
        } else if ("COACH".equalsIgnoreCase(role)) {
            return coachRepository.findById(entityId)
                    .map(c -> c.getUser() != null ? c.getUser().getFcmToken() : null)
                    .orElse(null);
        }
        return null;
    }
}
