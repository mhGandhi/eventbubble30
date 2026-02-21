package com.lennadi.eventbubble30.exceptions;

/**
 * ja so für besseres feedback und so ja
 */
public enum ErrorCodes {

    //401
    INVALID_CREDENTIALS("E_INVALID_CREDENTIALS"),
    EMAIL_NOT_VERIFIED("E_EMAIL_NOT_VERIFIED"),
    ACCOUNT_NOT_ACTIVE("E_ACCOUNT_NOT_ACTIVE"),
    LOG_IN_FIRST("E_LOG_IN_FIRST");

    public final String actual;
    ErrorCodes(String pActual){
        actual = pActual;
    }

    @Override
    public String toString() {
        return actual;
    }
}
