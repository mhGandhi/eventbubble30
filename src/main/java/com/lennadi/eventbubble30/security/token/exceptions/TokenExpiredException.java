package com.lennadi.eventbubble30.security.token.exceptions;

import com.lennadi.eventbubble30.security.AuthState;

public class TokenExpiredException extends TokenException {
    public TokenExpiredException() {
        super("Token has expired", AuthState.EXPIRED);
    }
}
