package com.lennadi.eventbubble30.security.token.exceptions;

import com.lennadi.eventbubble30.security.AuthState;
import com.lennadi.eventbubble30.security.token.TokenType;

public class WrongTokenTypeException extends TokenException {
    public WrongTokenTypeException(TokenType expected, TokenType actual) {
        super("Wrong token type. Expected '"+expected+"' but got '"+actual+"'", AuthState.WRONG_TOKEN_TYPE);
    }
}
