package com.lennadi.eventbubble30.security.captcha;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service()
@Profile("googleCaptcha")
public class GoogleCaptchaService implements CaptchaService {
    @Value("${captcha.secret}")
    private String secret;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
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
