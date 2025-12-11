package com.lennadi.eventbubble30.security.token.exceptions;

import com.lennadi.eventbubble30.security.AuthState;

public class TokenUserDoesNotExistException extends TokenException {
    public final long invalidId;
    public TokenUserDoesNotExistException(long pInvalidId) {
        super("User "+pInvalidId+" not found.", AuthState.USER_NOT_FOUND);
        this.invalidId = pInvalidId;
    }
}
