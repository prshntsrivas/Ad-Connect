package com.adconnect.chat.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document("conversations")
@Getter
@Setter
@NoArgsConstructor
public class Conversation {

    @Id
    private String id;

    /** "minUserId:maxUserId" - one conversation per pair of users. */
    @Indexed(unique = true)
    private String participantKey;

    @Indexed
    private List<Long> participantIds;

    private Map<String, String> participantNames = new HashMap<>();

    private Long dealId;

    private String lastMessagePreview;

    private Long lastSenderId;

    @Indexed
    private Instant lastMessageAt;

    /** Unread counter per participant id. */
    private Map<String, Integer> unread = new HashMap<>();

    private Instant createdAt;

    public static String keyFor(Long a, Long b) {
        return Math.min(a, b) + ":" + Math.max(a, b);
    }

    public boolean hasParticipant(Long userId) {
        return participantIds.contains(userId);
    }

    public Long otherParticipant(Long userId) {
        return participantIds.stream().filter(id -> !id.equals(userId)).findFirst().orElse(userId);
    }
}
