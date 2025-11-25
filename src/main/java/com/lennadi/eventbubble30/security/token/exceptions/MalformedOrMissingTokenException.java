package com.lennadi.eventbubble30.security.token.exceptions;

public class MalformedOrMissingTokenException extends TokenException {
    public MalformedOrMissingTokenException() {
        super("Malformed or Missing Token");
    }
}
