package com.veritas.backend.notification.service;

import com.veritas.backend.notification.dto.NotificationDto;
import com.veritas.backend.notification.entity.NotificationType;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void createNotification(User recipient, Request request, NotificationType type, String message);

    Page<NotificationDto> getNotificationsForUser(Long userId, Pageable pageable);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long notificationId, Long userId);

    boolean getEmailPreference(Long userId);

    void updateEmailPreference(Long userId, boolean enabled);
}
