package com.fitcoach.domain.enums;

/**
 * Category of an in-app notification. The string name is also sent in the FCM
 * data payload (lower-cased) so the Flutter client can pick an icon/accent.
 */
public enum NotificationType {
    GENERAL,
    WORKOUT,
    NUTRITION,
    MESSAGE,
    PROGRESS
}
