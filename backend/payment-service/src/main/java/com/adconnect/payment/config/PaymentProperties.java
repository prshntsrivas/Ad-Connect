package com.adconnect.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adconnect.payments")
public record PaymentProperties(String currency, int platformFeePercent, Stripe stripe) {

    public record Stripe(String secretKey, String publishableKey, String webhookSecret) {
    }

    public boolean stripeEnabled() {
        return stripe != null && stripe.secretKey() != null && !stripe.secretKey().isBlank();
    }
}
