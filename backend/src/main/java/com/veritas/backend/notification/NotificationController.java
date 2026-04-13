package com.veritas.backend.notification;

import com.veritas.backend.notification.dto.NotificationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification Module", description = "Notification retrieval and history for users")
public class NotificationController {
    @Operation(summary = "Get notifications", description = "Retrieves all notifications for a user.")
    @GetMapping
    public List<NotificationDto> getAllNotifications() {
        return List.of();
    }

    @Operation(summary = "Read notification", description = "Mark a notification as read.")
    @PatchMapping("/{id}")
    public String readNotification(@PathVariable Long id) {
        return "Marked notification " + id + " as read";
    }

    @Operation(summary = "Delete notification", description = "Deleted a notification.")
    @GetMapping("/{id}")
    public String deleteNotification(@PathVariable Long id) {
        return "Notification " + id + " deleted";
    }
}
