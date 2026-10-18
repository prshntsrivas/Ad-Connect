package com.adconnect.payment.gateway;

import com.adconnect.payment.entity.PaymentProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnExpression("'${adconnect.payments.stripe.secret-key:}'.isBlank()")
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.MOCK;
    }

    @Override
    public Intent createIntent(String idempotencyKey, BigDecimal amount, String currency, Long dealId, Long paymentId) {
        String id = "mock_" + UUID.randomUUID();
        log.info("Mock intent {} for deal {} amount {} {}", id, dealId, amount, currency);
        return new Intent(id, "mock_secret_" + paymentId);
    }

    @Override
    public void confirm(String providerPaymentId) {
        log.info("Mock confirm {}", providerPaymentId);
    }
}
