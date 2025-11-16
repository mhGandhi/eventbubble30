package com.lennadi.eventbubble30.security.captcha;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service()
@Profile("keinCaptcha")
public class KeinCaptchaService implements CaptchaService {
    /**
     * Gibt immer true zurück für Entwicklungszwecke. Deaktiviert Captcha effektiv.
     * @param t Token
     * @return Gültig?
     */
    @Override
    public boolean verify(String t) {
        return true;
    }
}
