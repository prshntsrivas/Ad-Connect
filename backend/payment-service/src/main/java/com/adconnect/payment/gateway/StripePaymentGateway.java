package com.adconnect.payment.gateway;

import com.adconnect.payment.config.PaymentProperties;
import com.adconnect.payment.entity.PaymentProvider;
import com.adconnect.common.web.ApiException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${adconnect.payments.stripe.secret-key:}'.isBlank()")
public class StripePaymentGateway implements PaymentGateway {

    private final PaymentProperties properties;

    @PostConstruct
    void init() {
        Stripe.apiKey = properties.stripe().secretKey();
        log.info("Stripe gateway enabled (test/live determined by the key prefix)");
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.STRIPE;
    }

    @Override
    public Intent createIntent(String idempotencyKey, BigDecimal amount, String currency, Long dealId, Long paymentId) {
        long minor = amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(minor)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true).build())
                .putMetadata("dealId", dealId.toString())
                .putMetadata("paymentId", paymentId.toString())
                .build();
        try {
            PaymentIntent intent = PaymentIntent.create(params,
                    com.stripe.net.RequestOptions.builder().setIdempotencyKey(idempotencyKey).build());
            return new Intent(intent.getId(), intent.getClientSecret());
        } catch (StripeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Stripe: " + e.getUserMessage());
        }
    }
}
