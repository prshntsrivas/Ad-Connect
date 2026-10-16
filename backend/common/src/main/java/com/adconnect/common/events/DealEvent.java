package com.adconnect.common.events;

import java.math.BigDecimal;
import java.time.Instant;

public record DealEvent(
        String eventId,
        Long dealId,
        Long campaignId,
        String campaignTitle,
        Long brandUserId,
        String brandEmail,
        String brandName,
        Long influencerUserId,
        String influencerEmail,
        String influencerName,
        BigDecimal amount,
        String previousStatus,
        String status,
        Instant occurredAt) {
}
