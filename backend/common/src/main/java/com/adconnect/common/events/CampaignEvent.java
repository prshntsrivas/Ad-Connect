package com.adconnect.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CampaignEvent(
        String eventId,
        Long campaignId,
        Long brandUserId,
        String brandName,
        String title,
        String description,
        String city,
        String niche,
        String platform,
        BigDecimal budget,
        String status,
        LocalDate deadline,
        Instant occurredAt) {
}
