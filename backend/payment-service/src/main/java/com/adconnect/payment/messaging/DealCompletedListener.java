package com.adconnect.payment.messaging;

import com.adconnect.common.events.DealEvent;
import com.adconnect.common.events.Topics;
import com.adconnect.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DealCompletedListener {

    private final PaymentService payments;

    @KafkaListener(topics = Topics.DEAL_STATUS)
    public void onDeal(DealEvent event) {
        if ("COMPLETED".equals(event.status())) {
            log.info("Deal {} completed, releasing escrow", event.dealId());
            payments.releaseOnDealCompleted(event.dealId());
        }
    }
}
