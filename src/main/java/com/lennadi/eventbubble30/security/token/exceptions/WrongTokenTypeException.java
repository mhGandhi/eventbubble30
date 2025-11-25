package com.lennadi.eventbubble30.security.token.exceptions;

public class WrongTokenTypeException extends TokenException {
    public WrongTokenTypeException(String expected, String actual) {
        super("Wrong token type. Expected '"+expected+"' but got '"+actual+"'");
    }
}
