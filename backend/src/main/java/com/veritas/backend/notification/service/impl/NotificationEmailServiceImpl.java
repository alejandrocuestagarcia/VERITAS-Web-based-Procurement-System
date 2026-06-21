package com.veritas.backend.notification.service.impl;

import com.veritas.backend.notification.entity.NotificationType;
import com.veritas.backend.notification.service.NotificationEmailService;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.veritas.backend.mail.MailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailServiceImpl implements NotificationEmailService {

    private final MailService mailService;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Override
    @Async
    public void sendNotificationEmail(User recipient, Request request, NotificationType type, String message) {
        if (!recipient.getNotificationEmailEnabled()) {
            log.info("Skipping notification email for {} — email notifications disabled by user", recipient.getEmail());
            return;
        }

        if (recipient.getRole() == UserRole.FINANCE_OFFICER) {
            log.info("Skipping notification email for Finance Officer {} [type={}]", recipient.getEmail(), type);
            return;
        }

        if ((type == NotificationType.ASSIGNED || type == NotificationType.SUBMITTED) && request != null) {
            if (request.getAssignee() == null || !request.getAssignee().getId().equals(recipient.getId())) {
                log.info("Skipping notification email for {} [type={}] — assignee is global/unassigned or does not match recipient", recipient.getEmail(), type);
                return;
            }
        }


        try {
            String subject = buildSubject(type, request);
            String body = buildBody(recipient, request, type, message);

            mailService.sendEmail(recipient.getEmail(), subject, body);
            log.info("Notification email queued for {} [type={}]", recipient.getEmail(), type);
        } catch (Exception e) {
            log.error("Failed to send notification email to {}: {}", recipient.getEmail(), e.getMessage(), e);
        }
    }

    private String buildSubject(NotificationType type, Request request) {
        String title = "N/A";
        if (request != null) {
            if (request.getRequestName() != null && !request.getRequestName().isBlank()) {
                title = request.getRequestName();
            } else if (request.getRequestKey() != null && !request.getRequestKey().isBlank()) {
                title = request.getRequestKey();
            }
        }
        return switch (type) {
            case SUBMITTED -> "VERITAS — New Request '" + title + "' Submitted";
            case APPROVED -> "VERITAS — Request '" + title + "' Approved";
            case REJECTED -> "VERITAS — Request '" + title + "' Rejected";
            case FINISHED -> "VERITAS — Request '" + title + "' Completed";
            case ASSIGNED -> "VERITAS — Request '" + title + "' Assigned to You";
            case REASSIGNED -> "VERITAS — Request '" + title + "' Reassigned";
            case PAID -> "VERITAS — Request '" + title + "' Paid";
            case REVERTED -> "VERITAS — Request '" + title + "' Sent Back";
        };
    }

    private String buildBody(User recipient, Request request, NotificationType type, String message) {
        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(recipient.getName()).append(",\n\n");
        body.append(message).append("\n\n");

        if (request != null) {
            body.append("Request Details:\n");
            body.append("  • Name: ").append(request.getRequestName()).append("\n");

            if (request.getState() == RequestStatus.FINISHED) {
                body.append("  • Status: Completed\n");
            } else if (request.getCurrentStep() != null) {
                body.append("  • Current Step: ").append(request.getCurrentStep().getName()).append("\n");
            }

            body.append("\nView this request: ")
                    .append(frontendBaseUrl)
                    .append("/requisitions/")
                    .append(request.getRequestID())
                    .append("\n");
        }

        body.append("\n---\n");
        body.append("You are receiving this email because you have email notifications enabled in VERITAS.\n");
        body.append("To disable email notifications, update your preferences in your user settings.\n");

        return body.toString();
    }
}
