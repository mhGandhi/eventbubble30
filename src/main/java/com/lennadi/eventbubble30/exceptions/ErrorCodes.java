package com.lennadi.eventbubble30.exceptions;

/**
 * ja so für besseres feedback und so ja
 */
public enum ErrorCodes {

    //401
    INVALID_CREDENTIALS("E_INVALID_CREDENTIALS"),
    EMAIL_NOT_VERIFIED("E_EMAIL_NOT_VERIFIED"),
    ACCOUNT_NOT_ACTIVE("E_ACCOUNT_NOT_ACTIVE"),
    LOG_IN_FIRST("E_LOG_IN_FIRST"),

    //403
    MOD_VIEW_NOT_ALLOWED("E_MOD_VIEW_NOT_ALLOWED"),
    BAD_CAPTCHA("E_BAD_CAPTCHA"),

    //409
    USERNAME_TAKEN("E_USERNAME_TAKEN"),
    EMAIL_TAKEN("E_EMAIL_TAKEN");//todo privacy concerns?

    public final String actual;
    ErrorCodes(String pActual){
        actual = pActual;
    }

    @Override
    public String toString() {
        return actual;
    }
}
