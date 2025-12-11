package com.lennadi.eventbubble30.security;

public enum AuthState {
    //standard
    AUTHENTICATED,
    NO_TOKEN,

    //abgelaufen
    EXPIRED,
    REVOKED,
    REVOKED_GLOBAL,
    PASSWORD_CHANGED,

    //Fehler
    EMPTY_TOKEN,
    MALFORMED,
    WRONG_AUTH_FORMAT,
    INVALID_SIGNATURE,
    WRONG_TOKEN_TYPE,
    USER_NOT_FOUND,

    UNKNOWN
}

