package com.lennadi.eventbubble30.security.token.exceptions;

import com.lennadi.eventbubble30.security.AuthState;

import java.time.Instant;

public class GlobalTokenRevokedException extends TokenRevokedException {
    public GlobalTokenRevokedException(Instant revokationTime)
    {
        super("All tokens have been revoked system wide and are invalid if older than "+revokationTime.toString() +". Please log in again.",revokationTime, AuthState.REVOKED_GLOBAL);
    }
}
