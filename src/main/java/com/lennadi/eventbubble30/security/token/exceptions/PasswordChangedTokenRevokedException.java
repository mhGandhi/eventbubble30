package com.lennadi.eventbubble30.security.token.exceptions;

import java.time.Instant;

public class PasswordChangedTokenRevokedException extends TokenRevokedException {
    public PasswordChangedTokenRevokedException(Instant pwChangeTime) {
        super("Your Tokens have been revoked after changing your password. Please log in again.", pwChangeTime);
    }
}
