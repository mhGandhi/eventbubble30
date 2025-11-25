package com.lennadi.eventbubble30.security.token.exceptions;

public class TokenExpiredException extends TokenException {
    public TokenExpiredException() {
        super("Token has expired");
    }
}
