package com.lennadi.eventbubble30.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CaptchaService {

    @Value("${captcha.secret}")
    private String secret;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verify(String token) {

        String url = "https://www.google.com/recaptcha/api/siteverify";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", secret);
        params.add("response", token);

        Map<String, Object> response =
                restTemplate.postForObject(url, params, Map.class);

        if (response == null)
            return false;

        Object success = response.get("success");
        return Boolean.TRUE.equals(success);
    }
}
