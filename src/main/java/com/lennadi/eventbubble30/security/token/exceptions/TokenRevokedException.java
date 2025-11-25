package com.lennadi.eventbubble30.security.token.exceptions;

import java.time.Instant;

public class TokenRevokedException extends TokenException {
    public final Instant revokationTime;
    public TokenRevokedException(String message, Instant pRevokationTime) {
        super(message);
        revokationTime = pRevokationTime;
    }
}
