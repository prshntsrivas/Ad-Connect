package com.adconnect.campaign.controller;

import com.adconnect.campaign.dto.DealDtos.DealPaymentView;
import com.adconnect.campaign.dto.DealDtos.FundedRequest;
import com.adconnect.campaign.service.DealService;
import com.adconnect.common.web.ApiException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Service-to-service API (called by payment-service via OpenFeign). Not routed by the gateway and
 * additionally protected by a shared internal token.
 */
@Hidden
@RestController
@RequestMapping("/internal/deals")
@RequiredArgsConstructor
public class InternalDealController {

    private final DealService deals;

    @Value("${adconnect.internal-token}")
    private String internalToken;

    @GetMapping("/{id}")
    public DealPaymentView get(@RequestHeader("X-Internal-Token") String token, @PathVariable Long id) {
        verify(token);
        return deals.paymentView(id);
    }

    @PostMapping("/{id}/funded")
    public DealPaymentView funded(@RequestHeader("X-Internal-Token") String token, @PathVariable Long id,
                                  @Valid @RequestBody FundedRequest request) {
        verify(token);
        return deals.markFunded(id, request.paymentId());
    }

    private void verify(String token) {
        if (!MessageDigest.isEqual(internalToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.forbidden("Invalid internal token");
        }
    }
}
