package com.teamforge.notifications;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record NotificationDto(Long id, String message, boolean isRead, LocalDateTime createdAt) {
    public static NotificationDto fromEntity(Notification notification) {
        return builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
