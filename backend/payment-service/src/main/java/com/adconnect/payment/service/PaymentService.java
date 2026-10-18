package com.adconnect.payment.service;

import com.adconnect.common.events.EventPublisher;
import com.adconnect.common.events.PaymentEvent;
import com.adconnect.common.events.Topics;
import com.adconnect.common.security.CurrentUser;
import com.adconnect.common.security.Role;
import com.adconnect.common.web.ApiException;
import com.adconnect.payment.client.CampaignClient;
import com.adconnect.payment.config.PaymentProperties;
import com.adconnect.payment.dto.PaymentDtos.PaymentDto;
import com.adconnect.payment.entity.Payment;
import com.adconnect.payment.entity.PaymentStatus;
import com.adconnect.payment.entity.PayoutStatus;
import com.adconnect.payment.gateway.PaymentGateway;
import com.adconnect.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final EnumSet<PaymentStatus> LIVE = EnumSet.of(PaymentStatus.CREATED, PaymentStatus.PROCESSING,
            PaymentStatus.SUCCEEDED);

    private final PaymentRepository payments;
    private final CampaignClient campaigns;
    private final PaymentGateway gateway;
    private final PaymentProperties properties;
    private final EventPublisher events;

    @Transactional
    public PaymentDto createIntent(CurrentUser user, Long dealId, String idempotencyKey) {
        user.require(Role.BRAND);
        return payments.findByIdempotencyKey(idempotencyKey)
                .map(this::toDto)
                .orElseGet(() -> createNew(user, dealId, idempotencyKey));
    }

    private PaymentDto createNew(CurrentUser user, Long dealId, String idempotencyKey) {
        payments.findFirstByDealIdAndStatusIn(dealId, LIVE).ifPresent(existing -> {
            throw ApiException.conflict("Deal already has a " + existing.getStatus() + " payment");
        });
        CampaignClient.DealView deal;
        try {
            deal = campaigns.getDeal(dealId);
        } catch (Exception e) {
            throw ApiException.notFound("Deal not found");
        }
        if (!deal.brandUserId().equals(user.id())) {
            throw ApiException.forbidden("Only the brand can fund this deal");
        }
        if (!"ACCEPTED".equals(deal.status())) {
            throw ApiException.conflict("Deal must be ACCEPTED before payment (currently " + deal.status() + ")");
        }

        BigDecimal fee = deal.amount()
                .multiply(BigDecimal.valueOf(properties.platformFeePercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        Payment payment = new Payment();
        payment.setDealId(dealId);
        payment.setBrandUserId(deal.brandUserId());
        payment.setInfluencerUserId(deal.influencerUserId());
        payment.setCampaignTitle(deal.campaignTitle());
        payment.setAmount(deal.amount());
        payment.setPlatformFee(fee);
        payment.setCurrency(properties.currency());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setProvider(gateway.provider());
        payment.setIdempotencyKey(idempotencyKey);
        payments.saveAndFlush(payment);

        PaymentGateway.Intent intent = gateway.createIntent(idempotencyKey, deal.amount(), properties.currency(),
                dealId, payment.getId());
        payment.setProviderPaymentId(intent.providerPaymentId());
        payment.setClientSecret(intent.clientSecret());
        payment.setStatus(PaymentStatus.PROCESSING);
        try {
            payments.flush();
        } catch (DataIntegrityViolationException race) {
            return payments.findByIdempotencyKey(idempotencyKey).map(this::toDto).orElseThrow();
        }
        return toDto(payment);
    }

    /** Confirms a mock payment (no Stripe Elements). Stripe uses the webhook instead. */
    @Transactional
    public PaymentDto confirmMock(CurrentUser user, Long paymentId) {
        Payment payment = payments.findById(paymentId).orElseThrow(() -> ApiException.notFound("Payment not found"));
        if (!payment.getBrandUserId().equals(user.id())) {
            throw ApiException.forbidden("Only the paying brand can confirm");
        }
        if (payment.getProvider() != com.adconnect.payment.entity.PaymentProvider.MOCK) {
            throw ApiException.badRequest("Stripe payments are confirmed by the client and webhook");
        }
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return toDto(payment);
        }
        gateway.confirm(payment.getProviderPaymentId());
        markSucceeded(payment);
        return toDto(payment);
    }

    @Transactional
    public void handleSucceeded(String providerPaymentId) {
        Payment payment = payments.findByProviderPaymentId(providerPaymentId)
                .orElseThrow(() -> ApiException.notFound("Payment " + providerPaymentId + " not found"));
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }
        markSucceeded(payment);
    }

    @Transactional
    public void handleFailed(String providerPaymentId, String reason) {
        payments.findByProviderPaymentId(providerPaymentId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
                return;
            }
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            publish(payment);
        });
    }

    @Transactional
    public void releaseOnDealCompleted(Long dealId) {
        payments.findFirstByDealIdAndStatusIn(dealId, EnumSet.of(PaymentStatus.SUCCEEDED)).ifPresent(payment -> {
            if (payment.getPayoutStatus() == PayoutStatus.HELD) {
                payment.setPayoutStatus(PayoutStatus.RELEASED);
                payment.setPayoutReleasedAt(Instant.now());
                log.info("Released payout for payment {} deal {}", payment.getId(), dealId);
            }
        });
    }

    @Transactional(readOnly = true)
    public PaymentDto get(CurrentUser user, Long id) {
        Payment payment = payments.findById(id).orElseThrow(() -> ApiException.notFound("Payment not found"));
        if (!payment.getBrandUserId().equals(user.id()) && !payment.getInfluencerUserId().equals(user.id())
                && !user.isStaff()) {
            throw ApiException.forbidden("Not a party to this payment");
        }
        return toDto(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> mine(CurrentUser user) {
        return payments.findForUser(user.id()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> forDeal(CurrentUser user, Long dealId) {
        List<Payment> list = payments.findByDealIdOrderByCreatedAtDesc(dealId);
        if (!list.isEmpty()) {
            Payment first = list.getFirst();
            if (!first.getBrandUserId().equals(user.id()) && !first.getInfluencerUserId().equals(user.id())
                    && !user.isStaff()) {
                throw ApiException.forbidden("Not a party to this payment");
            }
        }
        return list.stream().map(this::toDto).toList();
    }

    /** Retries notifying campaign-service if the earlier Feign call failed after funds were captured. */
    @Scheduled(fixedDelayString = "PT30S")
    @Transactional
    public void retryUnsynced() {
        payments.findByStatusAndDealSyncedFalse(PaymentStatus.SUCCEEDED).forEach(this::syncDeal);
    }

    private void markSucceeded(Payment payment) {
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPayoutStatus(PayoutStatus.HELD);
        publish(payment);
        syncDeal(payment);
    }

    private void syncDeal(Payment payment) {
        try {
            campaigns.markFunded(payment.getDealId(), new CampaignClient.FundedRequest(payment.getId()));
            payment.setDealSynced(true);
        } catch (Exception e) {
            log.warn("Could not mark deal {} as FUNDED (will retry): {}", payment.getDealId(), e.getMessage());
        }
    }

    private void publish(Payment payment) {
        events.publish(Topics.PAYMENT, payment.getId(), new PaymentEvent(UUID.randomUUID().toString(), payment.getId(),
                payment.getDealId(), payment.getBrandUserId(), payment.getInfluencerUserId(), payment.getAmount(),
                payment.getCurrency(), payment.getStatus().name(), payment.getProvider().name(),
                payment.getProviderPaymentId(), Instant.now()));
    }

    PaymentDto toDto(Payment p) {
        BigDecimal share = p.getAmount().subtract(p.getPlatformFee());
        String publishable = properties.stripe() == null ? null : properties.stripe().publishableKey();
        return new PaymentDto(p.getId(), p.getDealId(), p.getBrandUserId(), p.getInfluencerUserId(),
                p.getCampaignTitle(), p.getAmount(), p.getPlatformFee(), share, p.getCurrency(), p.getStatus(),
                p.getProvider(), p.getProviderPaymentId(), p.getClientSecret(), publishable, p.getPayoutStatus(),
                p.getPayoutReleasedAt(), p.getCreatedAt());
    }
}
