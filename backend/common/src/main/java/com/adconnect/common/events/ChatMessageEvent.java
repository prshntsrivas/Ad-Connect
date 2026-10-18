package com.adconnect.common.events;

import java.time.Instant;

public record ChatMessageEvent(
        String eventId,
        String conversationId,
        String messageId,
        Long senderId,
        String senderName,
        Long recipientId,
        String preview,
        Instant sentAt) {
}
