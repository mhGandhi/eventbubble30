package com.lennadi.eventbubble30.security.token.exceptions;

import com.lennadi.eventbubble30.security.AuthState;

public class TokenBadSignatureException extends TokenException {
    public TokenBadSignatureException() {
        super("JWT has an invalid signature.", AuthState.INVALID_SIGNATURE);
    }
}
