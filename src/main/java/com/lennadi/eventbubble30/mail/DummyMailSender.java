package com.lennadi.eventbubble30.mail;

import jakarta.mail.internet.MimeMessage;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@Profile("email-dummy")
public class DummyMailSender implements JavaMailSender {

    @Override
    public void send(SimpleMailMessage... messages) {
        for (SimpleMailMessage msg : messages) {
            System.out.println("=== Sending dummy email ===");
            System.out.println("To: " + String.join(", ", msg.getTo()));
            System.out.println("Subject: " + msg.getSubject());
            System.out.println("Text: " + msg.getText());
            System.out.println("===========================");
        }
    }

    @Override
    public MimeMessage createMimeMessage() {
        // Return a simple MimeMessage stub — must not be null
        return new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null);
    }

    @Override
    public MimeMessage createMimeMessage(InputStream stream) throws MailException{
        try {
            return new jakarta.mail.internet.MimeMessage(null, stream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(MimeMessage... mimeMessages) {
        System.out.println("DummyMailSender ignoring MimeMessages.");
    }
}
