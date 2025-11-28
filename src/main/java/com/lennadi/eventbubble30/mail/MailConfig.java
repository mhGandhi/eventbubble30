package com.lennadi.eventbubble30.mail;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class MailConfig {

    @Bean
    @Profile("EMAIL_dummy")
    public JavaMailSender localMailSender() {
        return new DummyMailSender();
    }
}
