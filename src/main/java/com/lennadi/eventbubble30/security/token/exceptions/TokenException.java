package com.lennadi.eventbubble30.security.token.exceptions;

import com.lennadi.eventbubble30.security.AuthState;

public class TokenException extends RuntimeException {
    public final AuthState authState;

    public TokenException(String message, AuthState pAuthState) {
        super(message);
        authState = pAuthState;
    }
}
