package com.adconnect.payment.gateway;

import com.adconnect.payment.entity.PaymentProvider;

import java.math.BigDecimal;

/** Stripe in production; the mock implementation when STRIPE_SECRET_KEY is empty. */
public interface PaymentGateway {

    record Intent(String providerPaymentId, String clientSecret) {
    }

    PaymentProvider provider();

    Intent createIntent(String idempotencyKey, BigDecimal amount, String currency, Long dealId, Long paymentId);

    /** Used by the mock provider; Stripe confirms on the client and notifies via webhook. */
    default void confirm(String providerPaymentId) {
        throw new UnsupportedOperationException("Confirm is only available in mock mode");
    }
}
