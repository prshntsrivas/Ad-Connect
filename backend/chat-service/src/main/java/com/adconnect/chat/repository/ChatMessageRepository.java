package com.adconnect.chat.repository;

import com.adconnect.chat.document.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    Optional<ChatMessage> findByConversationIdAndClientMessageId(String conversationId, String clientMessageId);

    List<ChatMessage> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    List<ChatMessage> findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(String conversationId, Instant before,
                                                                                Pageable pageable);
}
