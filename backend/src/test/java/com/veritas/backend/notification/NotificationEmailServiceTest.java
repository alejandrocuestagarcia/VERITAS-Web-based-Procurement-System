package com.veritas.backend.notification;

import com.veritas.backend.notification.entity.NotificationType;
import com.veritas.backend.notification.service.impl.NotificationEmailServiceImpl;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.mail.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEmailServiceTest {

    @Mock
    private MailService mailService;

    @InjectMocks
    private NotificationEmailServiceImpl emailService;

    private User recipient;
    private Request request;

    @BeforeEach
    void setUp() {
        recipient = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@veritas.com")
                .notificationEmailEnabled(true)
                .build();

        WorkflowStep step = new WorkflowStep();
        step.setName("Approval Step");

        request = new Request();
        request.setRequestID(42L);
        request.setRequestKey("REQ-42");
        request.setRequestName("Office Supplies");
        request.setCurrentStep(step);

        ReflectionTestUtils.setField(emailService, "frontendBaseUrl", "http://localhost:4200");
    }

    @Test
    void sendNotificationEmail_EmailNotificationsDisabled_Skipped() {
        recipient.setNotificationEmailEnabled(false);

        emailService.sendNotificationEmail(recipient, request, NotificationType.SUBMITTED, "Requisition submitted");

        verify(mailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendNotificationEmail_ValidRequest_SendsEmailWithDetails() {
        emailService.sendNotificationEmail(recipient, request, NotificationType.APPROVED, "Your request was approved");

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(mailService).sendEmail(toCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        String to = toCaptor.getValue();
        String subject = subjectCaptor.getValue();
        String body = bodyCaptor.getValue();

        assertAll(
            () -> assertEquals("alice@veritas.com", to),
            () -> assertEquals("VERITAS — Request 'Office Supplies' Approved", subject),
            () -> assertTrue(body.contains("Hello Alice,")),
            () -> assertTrue(body.contains("Your request was approved")),
            () -> assertTrue(body.contains("Request Details:")),
            () -> assertFalse(body.contains("Key:")),
            () -> assertTrue(body.contains("Name: Office Supplies")),
            () -> assertTrue(body.contains("Current Step: Approval Step")),
            () -> assertTrue(body.contains("http://localhost:4200/requisitions/42"))
        );
    }

    @Test
    void sendNotificationEmail_NullRequest_SendsBasicEmail() {
        emailService.sendNotificationEmail(recipient, null, NotificationType.FINISHED, "Requisition completed");

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(mailService).sendEmail(toCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        String to = toCaptor.getValue();
        String subject = subjectCaptor.getValue();
        String body = bodyCaptor.getValue();

        assertAll(
            () -> assertEquals("alice@veritas.com", to),
            () -> assertEquals("VERITAS — Request 'N/A' Completed", subject),
            () -> assertTrue(body.contains("Hello Alice,")),
            () -> assertTrue(body.contains("Requisition completed")),
            () -> assertFalse(body.contains("Request Details:")),
            () -> assertFalse(body.contains("http://localhost:4200/requisitions/"))
        );
    }

    @Test
    void sendNotificationEmail_RequestWithNullName_FallsBackToKey() {
        request.setRequestName(null);
        emailService.sendNotificationEmail(recipient, request, NotificationType.FINISHED, "Requisition completed");

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendEmail(anyString(), subjectCaptor.capture(), anyString());

        assertEquals("VERITAS — Request 'REQ-42' Completed", subjectCaptor.getValue());
    }

    @Test
    void sendNotificationEmail_MailSenderThrowsException_ExceptionIsCaught() {
        doThrow(new RuntimeException("Mail server down")).when(mailService).sendEmail(anyString(), anyString(), anyString());
        
        emailService.sendNotificationEmail(recipient, request, NotificationType.SUBMITTED, "Requisition submitted");

        verify(mailService).sendEmail(anyString(), anyString(), anyString());
    }
}
