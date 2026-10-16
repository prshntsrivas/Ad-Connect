package com.adconnect.payment.controller;

import com.adconnect.common.security.AuthUser;
import com.adconnect.common.security.CurrentUser;
import com.adconnect.payment.config.PaymentProperties;
import com.adconnect.payment.dto.PaymentDtos.ConfigDto;
import com.adconnect.payment.dto.PaymentDtos.ConfirmMockRequest;
import com.adconnect.payment.dto.PaymentDtos.CreateIntentRequest;
import com.adconnect.payment.dto.PaymentDtos.PaymentDto;
import com.adconnect.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Payments")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService payments;
    private final PaymentProperties properties;

    @GetMapping("/config")
    public ConfigDto config() {
        return new ConfigDto(properties.stripeEnabled(),
                properties.stripe() == null ? null : properties.stripe().publishableKey(),
                properties.currency(), properties.platformFeePercent());
    }

    @PostMapping("/intents")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDto createIntent(@AuthUser CurrentUser user, @Valid @RequestBody CreateIntentRequest request) {
        return payments.createIntent(user, request.dealId(), request.idempotencyKey());
    }

    @PostMapping("/mock/confirm")
    public PaymentDto confirmMock(@AuthUser CurrentUser user, @Valid @RequestBody ConfirmMockRequest request) {
        return payments.confirmMock(user, request.paymentId());
    }

    @GetMapping("/mine")
    public List<PaymentDto> mine(@AuthUser CurrentUser user) {
        return payments.mine(user);
    }

    @GetMapping("/{id}")
    public PaymentDto get(@AuthUser CurrentUser user, @PathVariable Long id) {
        return payments.get(user, id);
    }

    @GetMapping("/deal/{dealId}")
    public List<PaymentDto> forDeal(@AuthUser CurrentUser user, @PathVariable Long dealId) {
        return payments.forDeal(user, dealId);
    }
}
