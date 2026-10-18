package com.adconnect.notify.messaging;

import com.adconnect.common.events.ChatMessageEvent;
import com.adconnect.common.events.DealEvent;
import com.adconnect.common.events.PaymentEvent;
import com.adconnect.common.events.Topics;
import com.adconnect.common.events.UserRegisteredEvent;
import com.adconnect.common.events.UserStatusChangedEvent;
import com.adconnect.notify.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventListeners {

    private final NotificationDispatcher dispatcher;

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 3),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @KafkaListener(topics = Topics.USER_REGISTERED)
    public void onRegistered(UserRegisteredEvent e) {
        if (!dispatcher.claim(e.eventId(), Topics.USER_REGISTERED)) {
            return;
        }
        dispatcher.notifyUser(e.userId(), e.eventId(), "USER_REGISTERED",
                "Welcome to Ad-Connect",
                "Your account is pending review by a site manager.",
                "/profile", e.email(), "mail/welcome",
                Map.of("name", e.fullName(), "role", e.role()));
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 3),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @KafkaListener(topics = Topics.USER_STATUS_CHANGED)
    public void onStatus(UserStatusChangedEvent e) {
        if (!dispatcher.claim(e.eventId(), Topics.USER_STATUS_CHANGED)) {
            return;
        }
        String title = switch (e.status()) {
            case "VERIFIED" -> "Your account is verified";
            case "REJECTED" -> "Your account was not approved";
            case "SUSPENDED" -> "Your account has been suspended";
            default -> "Account status updated";
        };
        dispatcher.notifyUser(e.userId(), e.eventId(), "USER_STATUS", title,
                e.reason() == null ? title : e.reason(), "/profile", e.email(), "mail/status",
                Map.of("name", e.fullName(), "status", e.status(),
                        "reason", e.reason() == null ? "" : e.reason()));
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 3),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @KafkaListener(topics = Topics.DEAL_STATUS)
    public void onDeal(DealEvent e) {
        if (!dispatcher.claim(e.eventId(), Topics.DEAL_STATUS)) {
            return;
        }
        String title = "Deal #" + e.dealId() + " is now " + e.status();
        String link = "/deals/" + e.dealId();
        Map<String, Object> model = Map.of("campaignTitle", e.campaignTitle() == null ? "" : e.campaignTitle(),
                "status", e.status(), "amount", e.amount() == null ? "" : e.amount().toPlainString());
        if (e.brandEmail() != null) {
            dispatcher.notifyUser(e.brandUserId(), e.eventId() + ":brand", "DEAL", title,
                    e.campaignTitle(), link, e.brandEmail(), "mail/deal",
                    with(model, "name", e.brandName()));
        }
        if (e.influencerEmail() != null) {
            dispatcher.notifyUser(e.influencerUserId(), e.eventId() + ":inf", "DEAL", title,
                    e.campaignTitle(), link, e.influencerEmail(), "mail/deal",
                    with(model, "name", e.influencerName()));
        }
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 3),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @KafkaListener(topics = Topics.PAYMENT)
    public void onPayment(PaymentEvent e) {
        if (!dispatcher.claim(e.eventId(), Topics.PAYMENT)) {
            return;
        }
        String title = "Payment " + e.status() + " for deal #" + e.dealId();
        dispatcher.notifyUser(e.brandUserId(), e.eventId() + ":brand", "PAYMENT", title,
                e.amount() + " " + e.currency(), "/deals/" + e.dealId(), null, "mail/payment",
                Map.of("status", e.status(), "amount", e.amount(), "currency", e.currency(), "role", "brand"));
        dispatcher.notifyUser(e.influencerUserId(), e.eventId() + ":inf", "PAYMENT", title,
                e.amount() + " " + e.currency(), "/deals/" + e.dealId(), null, "mail/payment",
                Map.of("status", e.status(), "amount", e.amount(), "currency", e.currency(), "role", "influencer"));
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 3),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @KafkaListener(topics = Topics.CHAT_MESSAGE)
    public void onChat(ChatMessageEvent e) {
        if (!dispatcher.claim(e.eventId(), Topics.CHAT_MESSAGE)) {
            return;
        }
        dispatcher.notifyUser(e.recipientId(), e.eventId(), "CHAT",
                "New message from " + e.senderName(), e.preview(), "/chat/" + e.conversationId(),
                null, "mail/chat", Map.of("sender", e.senderName(), "preview", e.preview()));
    }

    @DltHandler
    public void dlt(Object payload) {
        log.error("Dead-lettered notification event: {}", payload);
    }

    private static Map<String, Object> with(Map<String, Object> base, String key, Object value) {
        java.util.HashMap<String, Object> copy = new java.util.HashMap<>(base);
        copy.put(key, value == null ? "" : value);
        return copy;
    }
}
