package com.adconnect.payment.repository;

import com.adconnect.payment.entity.Payment;
import com.adconnect.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    List<Payment> findByDealIdOrderByCreatedAtDesc(Long dealId);

    Optional<Payment> findFirstByDealIdAndStatusIn(Long dealId, Collection<PaymentStatus> statuses);

    List<Payment> findByStatusAndDealSyncedFalse(PaymentStatus status);

    @Query("select p from Payment p where p.brandUserId = :userId or p.influencerUserId = :userId order by p.createdAt desc")
    List<Payment> findForUser(@Param("userId") Long userId);
}
