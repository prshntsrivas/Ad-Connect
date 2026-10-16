package com.adconnect.campaign.dto;

import com.adconnect.campaign.entity.DealInitiator;
import com.adconnect.campaign.entity.DealStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class DealDtos {

    private DealDtos() {
    }

    public record ApplyRequest(
            @NotBlank @Size(max = 2000) String message,
            @NotNull @DecimalMin("1.0") BigDecimal amount) {
    }

    public record OfferRequest(
            @NotNull Long campaignId,
            @NotNull Long influencerUserId,
            @NotNull @DecimalMin("1.0") BigDecimal amount,
            @Size(max = 2000) String message) {
    }

    public record NoteRequest(@Size(max = 1000) String note) {
    }

    public record DeliverRequest(
            @NotBlank @Size(max = 500) @Pattern(regexp = "^https?://.+", message = "must be an http(s) URL")
            String deliverableUrl,
            @Size(max = 2000) String note) {
    }

    public record ReviewRequest(@Min(1) @Max(5) int rating, @Size(max = 2000) String comment) {
    }

    public record HistoryDto(DealStatus fromStatus, DealStatus toStatus, Long changedBy, String note,
                             Instant createdAt) {
    }

    public record DealDto(Long id, Long campaignId, String campaignTitle, String brandName, Long brandUserId,
                          Long influencerUserId, String influencerName, DealInitiator initiatedBy, String message,
                          BigDecimal amount, DealStatus status, Long paymentId, String deliverableUrl,
                          String deliveryNote, List<HistoryDto> history, List<ReviewDto> reviews,
                          Instant createdAt, Instant updatedAt) {
    }

    public record ReviewDto(Long id, Long dealId, Long reviewerUserId, Long revieweeUserId, String reviewerName,
                            int rating, String comment, Instant createdAt) {
    }

    /** Minimal view used by payment-service over the internal API. */
    public record DealPaymentView(Long id, Long campaignId, String campaignTitle, Long brandUserId,
                                  Long influencerUserId, BigDecimal amount, DealStatus status, Long paymentId) {
    }

    public record FundedRequest(@NotNull Long paymentId) {
    }
}
