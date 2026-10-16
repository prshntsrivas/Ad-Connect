package com.adconnect.common.events;

import java.time.Instant;

public record UserStatusChangedEvent(
        String eventId,
        Long userId,
        String email,
        String fullName,
        String role,
        String status,
        String reason,
        Instant occurredAt) {
}
