package com.veritas.backend.mail.service;

import com.veritas.backend.mail.impl.MailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailServiceImpl mailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "fromAddress", "no-reply@veritas.com");
    }

    @Test
    void sendEmail_MailNotConfigured_Skipped() {
        ReflectionTestUtils.setField(mailService, "fromAddress", "");

        mailService.sendEmail("test@example.com", "Subject", "Body");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_MailNullConfigured_Skipped() {
        ReflectionTestUtils.setField(mailService, "fromAddress", null);

        mailService.sendEmail("test@example.com", "Subject", "Body");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_ValidParameters_SendsSuccessfully() {
        mailService.sendEmail("test@example.com", "Subject", "Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertAll(
            () -> assertEquals("no-reply@veritas.com", sentMessage.getFrom()),
            () -> assertArrayEquals(new String[]{"test@example.com"}, sentMessage.getTo()),
            () -> assertEquals("Subject", sentMessage.getSubject()),
            () -> assertEquals("Body", sentMessage.getText())
        );
    }

    @Test
    void sendEmail_SenderThrowsException_ExceptionIsCaught() {
        doThrow(new RuntimeException("SMTP Server offline")).when(mailSender).send(any(SimpleMailMessage.class));

        mailService.sendEmail("test@example.com", "Subject", "Body");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
