package com.adconnect.campaign.dto;

import com.adconnect.campaign.entity.CampaignStatus;
import com.adconnect.campaign.entity.Platform;
import com.adconnect.campaign.validation.ValidDateRange;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class CampaignDtos {

    private CampaignDtos() {
    }

    @ValidDateRange
    public record CampaignRequest(
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 4000) String description,
            @Size(max = 2000) String deliverables,
            @NotBlank @Size(max = 40) String niche,
            @NotNull Platform platform,
            @Size(max = 80) String city,
            @NotNull @DecimalMin(value = "100.0", message = "must be at least 100") BigDecimal budget,
            @PositiveOrZero long minFollowers,
            LocalDate startDate,
            LocalDate endDate,
            @FutureOrPresent LocalDate applicationDeadline) {
    }

    public record CampaignStatusRequest(@NotNull CampaignStatus status) {
    }

    public record CampaignDto(Long id, Long brandUserId, String brandName, String title, String description,
                              String deliverables, String niche, Platform platform, String city, BigDecimal budget,
                              long minFollowers, CampaignStatus status, LocalDate startDate, LocalDate endDate,
                              LocalDate applicationDeadline, boolean acceptingApplications, Instant createdAt) {
    }
}
