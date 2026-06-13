package com.veritas.backend.mail;

public interface MailService {

    /**
     * Sends a plain-text email asynchronously.
     * If the sender address is not configured, the email is silently skipped.
     * Delivery failures are logged but not propagated to the caller.
     *
     * @param to the recipient email address
     * @param subject the email subject line
     * @param body the plain-text email body
     */
    void sendEmail(String to, String subject, String body);
}
