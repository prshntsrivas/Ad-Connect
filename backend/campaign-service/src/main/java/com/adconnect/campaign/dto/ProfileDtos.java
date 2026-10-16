package com.adconnect.campaign.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record BrandProfileRequest(
            @NotBlank @Size(max = 150) String companyName,
            @Size(max = 80) String industry,
            @Size(max = 255) String website,
            @Size(max = 80) String city,
            @Size(max = 80) String state,
            @Size(max = 2000) String description,
            @Size(max = 500) String logoUrl) {
    }

    public record BrandProfileDto(Long userId, String companyName, String industry, String website, String city,
                                  String state, String description, String logoUrl, Instant createdAt) {
    }

    public record InfluencerProfileRequest(
            @NotBlank @Size(max = 120) String displayName,
            @Size(max = 2000) String bio,
            @NotBlank @Size(max = 80) String city,
            @Size(max = 80) String state,
            @Size(max = 80) String country,
            @Size(max = 500) String photoUrl,
            @NotEmpty @Size(max = 6) Set<@NotBlank @Size(max = 40) String> niches,
            @Size(max = 80) String instagramHandle,
            @PositiveOrZero long instagramFollowers,
            @Size(max = 120) String youtubeChannel,
            @PositiveOrZero long youtubeSubscribers,
            @Size(max = 80) String twitterHandle,
            @PositiveOrZero long twitterFollowers,
            @DecimalMin("0.0") @DecimalMax("100.0") double engagementRate,
            @NotNull @DecimalMin("0.0") BigDecimal pricePerPost) {
    }

    public record InfluencerProfileDto(Long userId, String displayName, String bio, String city, String state,
                                       String country, String photoUrl, List<String> niches, String instagramHandle,
                                       long instagramFollowers, String youtubeChannel, long youtubeSubscribers,
                                       String twitterHandle, long twitterFollowers, long totalFollowers,
                                       List<String> platforms, double engagementRate, BigDecimal pricePerPost,
                                       double rating, int ratingCount, int completedDeals, boolean verified,
                                       Instant createdAt) {
    }

    /** Whichever profile the caller owns, depending on role. */
    public record MyProfileResponse(String role, BrandProfileDto brand, InfluencerProfileDto influencer) {
    }
}
