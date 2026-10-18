package com.adconnect.payment.entity;

public enum PaymentProvider {
    STRIPE,
    /** Local simulation used when no Stripe key is configured. */
    MOCK
}
