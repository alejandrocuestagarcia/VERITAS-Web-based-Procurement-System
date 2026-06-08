package com.veritas.backend.mail;

public interface MailService {
    void sendEmail(String to, String subject, String body);
}
