package com.adconnect.common.events;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentEvent(
        String eventId,
        Long paymentId,
        Long dealId,
        Long brandUserId,
        Long influencerUserId,
        BigDecimal amount,
        String currency,
        String status,
        String provider,
        String providerReference,
        Instant occurredAt) {
}
