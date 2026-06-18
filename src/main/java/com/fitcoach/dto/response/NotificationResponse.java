package com.fitcoach.dto.response;

import com.fitcoach.domain.entity.Notification;
import com.fitcoach.domain.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String body;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
