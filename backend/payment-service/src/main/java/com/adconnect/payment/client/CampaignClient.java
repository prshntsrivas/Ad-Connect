package com.adconnect.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

/** Declarative REST client resolved through Eureka ("campaign-service") with client-side load balancing. */
@FeignClient(name = "campaign-service", path = "/internal/deals", configuration = InternalAuthConfig.class)
public interface CampaignClient {

    @GetMapping("/{id}")
    DealView getDeal(@PathVariable("id") Long id);

    @PostMapping("/{id}/funded")
    DealView markFunded(@PathVariable("id") Long id, @RequestBody FundedRequest request);

    record DealView(Long id, Long campaignId, String campaignTitle, Long brandUserId, Long influencerUserId,
                    BigDecimal amount, String status, Long paymentId) {
    }

    record FundedRequest(Long paymentId) {
    }
}
