package com.lennadi.eventbubble30.security.token.exceptions;

import com.lennadi.eventbubble30.security.AuthState;

import java.time.Instant;

public class TokenRevokedException extends TokenException {
    public final Instant revokationTime;
    public TokenRevokedException(String message, Instant pRevokationTime, AuthState pAuthState) {
        super(message, pAuthState);
        revokationTime = pRevokationTime;
    }
}
