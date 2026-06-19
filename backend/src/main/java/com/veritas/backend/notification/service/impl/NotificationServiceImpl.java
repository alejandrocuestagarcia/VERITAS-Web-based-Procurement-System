package com.veritas.backend.notification.service.impl;

import com.veritas.backend.notification.dto.NotificationDto;
import com.veritas.backend.notification.entity.Notification;
import com.veritas.backend.notification.entity.NotificationType;
import com.veritas.backend.notification.mapper.NotificationMapper;
import com.veritas.backend.notification.repository.NotificationRepository;
import com.veritas.backend.notification.service.NotificationService;
import com.veritas.backend.notification.service.NotificationEmailService;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.veritas.backend.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationEmailService notificationEmailService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void createNotification(User recipient, Request request, NotificationType type, String message) {
        Notification notification = Notification.builder()
                .user(recipient)
                .message(message)
                .type(type)
                .request(request)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("Notification created for user {} [type={}]: {}", recipient.getEmail(), type, message);

        notificationEmailService.sendNotificationEmail(recipient, request, type, message);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsForUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to access this notification");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to delete this notification");
        }

        notificationRepository.delete(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getEmailPreference(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        return user.getNotificationEmailEnabled();
    }

    @Override
    @Transactional
    public void updateEmailPreference(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        user.setNotificationEmailEnabled(enabled);
        userRepository.save(user);
    }
}
