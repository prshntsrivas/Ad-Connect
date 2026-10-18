package com.adconnect.payment.controller;

import com.adconnect.payment.config.PaymentProperties;
import com.adconnect.payment.entity.ProcessedWebhookEvent;
import com.adconnect.payment.repository.ProcessedWebhookEventRepository;
import com.adconnect.payment.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@Slf4j
@RestController
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentProperties properties;
    private final PaymentService payments;
    private final ProcessedWebhookEventRepository processed;

    @PostMapping("/api/payments/webhook/stripe")
    public ResponseEntity<Void> stripe(@RequestBody String payload,
                                       @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        String secret = properties.stripe() == null ? null : properties.stripe().webhookSecret();
        if (secret == null || secret.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, secret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try {
            processed.save(new ProcessedWebhookEvent(event.getId(), event.getType()));
        } catch (DataIntegrityViolationException duplicate) {
            return ResponseEntity.ok().build();
        }

        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
        if (object instanceof PaymentIntent intent) {
            switch (event.getType()) {
                case "payment_intent.succeeded" -> payments.handleSucceeded(intent.getId());
                case "payment_intent.payment_failed" ->
                        payments.handleFailed(intent.getId(), intent.getLastPaymentError() == null
                                ? "failed" : intent.getLastPaymentError().getMessage());
                default -> log.debug("Ignored Stripe event {}", event.getType());
            }
        }
        return ResponseEntity.ok().build();
    }
}
