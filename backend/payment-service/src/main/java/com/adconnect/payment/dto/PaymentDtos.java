package com.adconnect.payment.dto;

import com.adconnect.payment.entity.PaymentProvider;
import com.adconnect.payment.entity.PaymentStatus;
import com.adconnect.payment.entity.PayoutStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record CreateIntentRequest(@NotNull Long dealId, @NotBlank @Size(min = 8, max = 100) String idempotencyKey) {
    }

    public record ConfirmMockRequest(@NotNull Long paymentId) {
    }

    public record PaymentDto(Long id, Long dealId, Long brandUserId, Long influencerUserId, String campaignTitle,
                             BigDecimal amount, BigDecimal platformFee, BigDecimal influencerShare, String currency,
                             PaymentStatus status, PaymentProvider provider, String providerPaymentId,
                             String clientSecret, String publishableKey, PayoutStatus payoutStatus,
                             Instant payoutReleasedAt, Instant createdAt) {
    }

    public record ConfigDto(boolean stripeEnabled, String publishableKey, String currency, int platformFeePercent) {
    }
}
