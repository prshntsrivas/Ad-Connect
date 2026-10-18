package com.adconnect.campaign.messaging;

import com.adconnect.campaign.service.ProfileService;
import com.adconnect.common.events.Topics;
import com.adconnect.common.events.UserStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatusListener {

    private final ProfileService profiles;
    private final CacheManager cacheManager;

    @KafkaListener(topics = Topics.USER_STATUS_CHANGED)
    public void onStatusChanged(UserStatusChangedEvent event) {
        log.info("User {} is now {}", event.userId(), event.status());
        Optional.ofNullable(cacheManager.getCache("users")).ifPresent(c -> c.evict(event.userId()));
        if ("INFLUENCER".equals(event.role())) {
            profiles.syncVerification(event.userId(), "VERIFIED".equals(event.status()));
        }
    }
}
