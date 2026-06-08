package com.veritas.backend.notification.service;

import com.veritas.backend.notification.entity.NotificationType;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;

public interface NotificationEmailService {

    void sendNotificationEmail(User recipient, Request request, NotificationType type, String message);
}
