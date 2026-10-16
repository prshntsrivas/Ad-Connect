package com.adconnect.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long dealId;

    @Column(nullable = false)
    private Long brandUserId;

    @Column(nullable = false)
    private Long influencerUserId;

    private String campaignTitle;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal platformFee;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider;

    private String providerPaymentId;

    private String clientSecret;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutStatus payoutStatus = PayoutStatus.NOT_STARTED;

    private Instant payoutReleasedAt;

    /** False until campaign-service has acknowledged the funded deal; retried by a scheduler. */
    private boolean dealSynced;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public boolean isOpen() {
        return status == PaymentStatus.CREATED || status == PaymentStatus.PROCESSING;
    }
}
