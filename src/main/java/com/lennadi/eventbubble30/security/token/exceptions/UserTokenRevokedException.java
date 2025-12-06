package com.lennadi.eventbubble30.security.token.exceptions;

import com.lennadi.eventbubble30.security.AuthState;

import java.time.Instant;

public class UserTokenRevokedException extends TokenRevokedException {
    public UserTokenRevokedException(Instant revokationTime) {
        super("Your Tokens have been revoked and are invalid if older than "+revokationTime.toString()+". Please log in again.", revokationTime, AuthState.REVOKED);
    }
}
