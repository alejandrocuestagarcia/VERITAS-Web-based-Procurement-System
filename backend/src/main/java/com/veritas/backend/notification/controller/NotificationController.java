package com.veritas.backend.notification.controller;

import com.veritas.backend.notification.dto.NotificationDto;
import com.veritas.backend.notification.service.NotificationService;
import com.veritas.backend.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification Module", description = "Notification retrieval and management for users")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get notifications", description = "Retrieves paginated notifications for the authenticated user.")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<NotificationDto> getAllNotifications(@AuthenticationPrincipal User user, Pageable pageable) {
        log.info("GET /notifications - user: {}, pageable: {}", user.getEmail(), pageable);
        return notificationService.getNotificationsForUser(user.getId(), pageable);
    }

    @Operation(summary = "Get unread count", description = "Returns the number of unread notifications for the authenticated user.")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/unread-count", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal User user) {
        log.info("GET /notifications/unread-count - user: {}", user.getEmail());
        return ResponseEntity.ok(notificationService.getUnreadCount(user.getId()));
    }

    @Operation(summary = "Mark notification as read", description = "Marks a single notification as read.")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping(value = "/{id}/read", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
        log.info("PATCH /notifications/{}/read - user: {}", id, user.getEmail());
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark all as read", description = "Marks all notifications as read for the authenticated user.")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping(value = "/read-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal User user) {
        log.info("PATCH /notifications/read-all - user: {}", user.getEmail());
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete notification", description = "Deletes a notification.")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id, @AuthenticationPrincipal User user) {
        log.info("DELETE /notifications/{} - user: {}", id, user.getEmail());
        notificationService.deleteNotification(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get email notification preference", description = "Gets the email notification status for the authenticated user.")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/email-preference", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> getEmailPreference(@AuthenticationPrincipal User user) {
        log.info("GET /notifications/email-preference - user: {}", user.getEmail());
        return ResponseEntity.ok(notificationService.getEmailPreference(user.getId()));
    }

    @Operation(summary = "Update email notification preference", description = "Updates the email notification status for the authenticated user.")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping(value = "/email-preference", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateEmailPreference(@AuthenticationPrincipal User user, @RequestParam boolean enabled) {
        log.info("PATCH /notifications/email-preference - user: {}, enabled: {}", user.getEmail(), enabled);
        notificationService.updateEmailPreference(user.getId(), enabled);
        return ResponseEntity.noContent().build();
    }
}
