package com.teamforge.notifications;

import com.teamforge.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<NotificationDto>> getMyNotifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList()));
    }

    @PutMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Notification not found"));
                
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Not authorized to read this notification");
        }
        
        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok().build();
    }
}
