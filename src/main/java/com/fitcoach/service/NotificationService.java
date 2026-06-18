package com.fitcoach.service;

import com.fitcoach.domain.entity.Notification;
import com.fitcoach.domain.entity.User;
import com.fitcoach.domain.enums.NotificationType;
import com.fitcoach.dto.response.NotificationResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.NotificationRepository;
import com.fitcoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Persists in-app notifications and (best-effort) pushes them via FCM.
 * Backs the notification inbox and unread badge (SCRUM-13 / SCRUM-84).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FirebaseNotificationService firebaseNotificationService;

    /**
     * Stores a notification for {@code recipient} and sends a push to their device.
     * Safe to call with a null recipient (no-op) so callers don't need null checks.
     */
    @Transactional
    public void notify(User recipient, NotificationType type, String title, String body) {
        if (recipient == null) return;

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type != null ? type : NotificationType.GENERAL)
                .title(title)
                .body(body)
                .read(false)
                .build();
        notificationRepository.save(notification);

        firebaseNotificationService.sendToToken(
                recipient.getFcmToken(), title, body,
                (type != null ? type : NotificationType.GENERAL).name());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getForUser(String email) {
        User user = requireUser(email);
        return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        User user = requireUser(email);
        return notificationRepository.countByRecipient_IdAndReadIsFalse(user.getId());
    }

    @Transactional
    public void markRead(String email, Long notificationId) {
        User user = requireUser(email);
        Notification n = notificationRepository
                .findByIdAndRecipient_Id(notificationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.isRead()) {
            n.setRead(true);
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void markAllRead(String email) {
        User user = requireUser(email);
        notificationRepository.markAllReadForRecipient(user.getId());
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
