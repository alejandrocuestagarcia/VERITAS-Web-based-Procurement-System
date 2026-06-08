package com.veritas.backend.mail;

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

import static org.assertj.core.api.Assertions.assertThat;
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
        assertThat(sentMessage.getFrom()).isEqualTo("no-reply@veritas.com");
        assertThat(sentMessage.getTo()).containsExactly("test@example.com");
        assertThat(sentMessage.getSubject()).isEqualTo("Subject");
        assertThat(sentMessage.getText()).isEqualTo("Body");
    }

    @Test
    void sendEmail_SenderThrowsException_ExceptionIsCaught() {
        doThrow(new RuntimeException("SMTP Server offline")).when(mailSender).send(any(SimpleMailMessage.class));

        mailService.sendEmail("test@example.com", "Subject", "Body");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
