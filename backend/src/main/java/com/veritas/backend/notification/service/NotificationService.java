package com.veritas.backend.notification.service;

import com.veritas.backend.notification.dto.NotificationDto;
import com.veritas.backend.notification.entity.NotificationType;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    /**
     * Creates an in-app notification for the given recipient and triggers a notification email.
     *
     * @param recipient the user to notify
     * @param request the request associated with the notification
     * @param type the notification type
     * @param message the notification message
     */
    void createNotification(User recipient, Request request, NotificationType type, String message);

    /**
     * Returns a paginated list of notifications for the given user, ordered by creation date descending.
     *
     * @param userId the ID of the user
     * @param pageable pagination and sorting parameters
     * @return a page of {@link NotificationDto}
     */
    Page<NotificationDto> getNotificationsForUser(Long userId, Pageable pageable);

    /**
     * Returns the number of unread notifications for the given user.
     *
     * @param userId the ID of the user
     * @return the unread notification count
     */
    long getUnreadCount(Long userId);

    /**
     * Marks a single notification as read. Only the owning user may mark their own notifications.
     *
     * @param notificationId the ID of the notification to mark as read
     * @param userId the ID of the requesting user
     * @throws EntityNotFoundException if the notification does not exist
     * @throws AccessDeniedException if the notification belongs to a different user
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * Marks all notifications for the given user as read.
     *
     * @param userId the ID of the user
     */
    void markAllAsRead(Long userId);

    /**
     * Deletes a notification. Only the owning user may delete their own notifications.
     *
     * @param notificationId the ID of the notification to delete
     * @param userId the ID of the requesting user
     * @throws EntityNotFoundException if the notification does not exist
     * @throws AccessDeniedException if the notification belongs to a different user
     */
    void deleteNotification(Long notificationId, Long userId);

    /**
     * Returns whether email notifications are enabled for the given user.
     *
     * @param userId the ID of the user
     * @return {@code true} if email notifications are enabled, {@code false} otherwise
     * @throws EntityNotFoundException if no user exists with the given ID
     */
    boolean getEmailPreference(Long userId);

    /**
     * Updates the email notification preference for the given user.
     *
     * @param userId the ID of the user
     * @param enabled {@code true} to enable email notifications, {@code false} to disable
     * @throws EntityNotFoundException if no user exists with the given ID
     */
    void updateEmailPreference(Long userId, boolean enabled);
}
