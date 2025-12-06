package com.lennadi.eventbubble30.security;

public enum AuthState {
    AUTHENTICATED,
    NO_TOKEN,

    EMPTY_TOKEN,
    MALFORMED,
    WRONG_AUTH_FORMAT,

    INVALID_SIGNATURE,

    EXPIRED,
    REVOKED,
    REVOKED_GLOBAL,
    PASSWORD_CHANGED,

    WRONG_TOKEN_TYPE,

    USER_NOT_FOUND,

    UNKNOWN
}

