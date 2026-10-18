package com.adconnect.auth.entity;

public enum UserStatus {
    /** Registered, awaiting review by a site manager. */
    PENDING,
    VERIFIED,
    REJECTED,
    SUSPENDED
}
