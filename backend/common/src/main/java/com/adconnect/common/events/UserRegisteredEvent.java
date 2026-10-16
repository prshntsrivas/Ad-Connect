package com.adconnect.common.events;

import java.time.Instant;

public record UserRegisteredEvent(
        String eventId,
        Long userId,
        String email,
        String fullName,
        String role,
        Instant occurredAt) {
}
