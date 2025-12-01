package com.lennadi.eventbubble30.security;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenGeneration {
    public static String generateVerificationToken() {
        return secureToken(24);
    }

    public static String generatePasswordResetToken() {
        return secureToken(32);
    }

    private static String secureToken(int lenBytes){
        byte[] bytes = new byte[lenBytes];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
