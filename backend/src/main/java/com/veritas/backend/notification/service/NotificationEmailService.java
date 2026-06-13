package com.veritas.backend.notification.service;

import com.veritas.backend.notification.entity.NotificationType;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;

public interface NotificationEmailService {

    /**
     * Sends a notification email to the recipient asynchronously.
     * Skipped silently if the recipient has email notifications disabled.
     * Failures are logged but not propagated to the caller.
     *
     * @param recipient the user to send the email to
     * @param request the request associated with the notification, used to build the email subject and body
     * @param type the notification type, determines the email subject line
     * @param message the notification message included in the email body
     */
    void sendNotificationEmail(User recipient, Request request, NotificationType type, String message);
}
