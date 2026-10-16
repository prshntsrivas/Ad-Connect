package com.adconnect.chat.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("messages")
@CompoundIndexes({
        // Idempotency backstop: a client retry with the same clientMessageId can never create a duplicate.
        @CompoundIndex(name = "uk_conversation_client_msg", def = "{'conversationId': 1, 'clientMessageId': 1}", unique = true),
        @CompoundIndex(name = "idx_conversation_created", def = "{'conversationId': 1, 'createdAt': -1}")
})
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    private String id;

    private String conversationId;

    private Long senderId;

    private String senderName;

    private String content;

    private String clientMessageId;

    private Instant createdAt;
}
