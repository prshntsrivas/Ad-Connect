package com.adconnect.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InfluencerProfileEvent(
        String eventId,
        Long userId,
        String displayName,
        String city,
        String state,
        String country,
        List<String> niches,
        List<String> platforms,
        long instagramFollowers,
        long youtubeSubscribers,
        long twitterFollowers,
        long totalFollowers,
        double engagementRate,
        BigDecimal pricePerPost,
        String bio,
        double rating,
        int completedDeals,
        boolean verified,
        Instant occurredAt) {
}
