package com.adconnect.chat.repository;

import com.adconnect.chat.document.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    Optional<Conversation> findByParticipantKey(String participantKey);

    List<Conversation> findByParticipantIdsContainsOrderByLastMessageAtDesc(Long userId);
}
