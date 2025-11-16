package com.lennadi.eventbubble30.security.captcha;

import org.springframework.stereotype.Service;

@Service
public interface CaptchaService {
    public boolean verify(String t);
}
