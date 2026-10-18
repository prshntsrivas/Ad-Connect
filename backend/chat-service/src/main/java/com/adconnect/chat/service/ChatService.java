package com.adconnect.chat.service;

import com.adconnect.chat.client.UserNameClient;
import com.adconnect.chat.document.ChatMessage;
import com.adconnect.chat.document.Conversation;
import com.adconnect.chat.dto.ChatDtos.ConversationDto;
import com.adconnect.chat.dto.ChatDtos.MessageDto;
import com.adconnect.chat.dto.ChatDtos.SendResult;
import com.adconnect.chat.repository.ChatMessageRepository;
import com.adconnect.chat.repository.ConversationRepository;
import com.adconnect.common.events.ChatMessageEvent;
import com.adconnect.common.events.EventPublisher;
import com.adconnect.common.events.Topics;
import com.adconnect.common.security.CurrentUser;
import com.adconnect.common.web.ApiException;
import com.adconnect.grpc.user.UserReply;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final MongoTemplate mongo;
    private final StringRedisTemplate redis;
    private final UserNameClient users;
    private final EventPublisher events;
    private final ChatFanout fanout;

    @Value("${adconnect.chat.idempotency-ttl}")
    private Duration idempotencyTtl;

    public ConversationDto startConversation(CurrentUser me, Long participantId, Long dealId) {
        if (participantId.equals(me.id())) {
            throw ApiException.badRequest("Cannot start a conversation with yourself");
        }
        String key = Conversation.keyFor(me.id(), participantId);
        Conversation conversation = conversations.findByParticipantKey(key).orElseGet(() -> {
            UserReply other = users.get(participantId);
            Conversation created = new Conversation();
            created.setParticipantKey(key);
            created.setParticipantIds(List.of(me.id(), participantId));
            created.getParticipantNames().put(me.id().toString(), me.name());
            created.getParticipantNames().put(participantId.toString(), other.getFullName());
            created.setDealId(dealId);
            created.setCreatedAt(Instant.now());
            created.setLastMessageAt(created.getCreatedAt());
            try {
                return conversations.save(created);
            } catch (DuplicateKeyException race) {
                return conversations.findByParticipantKey(key).orElseThrow();
            }
        });
        return toDto(conversation, me.id());
    }

    public List<ConversationDto> myConversations(CurrentUser me) {
        return conversations.findByParticipantIdsContainsOrderByLastMessageAtDesc(me.id()).stream()
                .map(c -> toDto(c, me.id()))
                .toList();
    }

    public List<MessageDto> history(CurrentUser me, String conversationId, Instant before, int limit) {
        requireParticipant(me, conversationId);
        PageRequest page = PageRequest.of(0, Math.min(Math.max(limit, 1), 100));
        List<ChatMessage> list = before == null
                ? messages.findByConversationIdOrderByCreatedAtDesc(conversationId, page)
                : messages.findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(conversationId, before, page);
        List<MessageDto> result = new ArrayList<>(list.stream().map(ChatService::toDto).toList());
        Collections.reverse(result);
        return result;
    }

    /**
     * Idempotent send. A Redis lookup short-circuits retries cheaply; the unique
     * (conversationId, clientMessageId) index guarantees no duplicate even when two retries race.
     */
    public SendResult send(CurrentUser me, String conversationId, String clientMessageId, String content) {
        Conversation conversation = requireParticipant(me, conversationId);
        String idemKey = "chat:idem:" + conversationId + ":" + clientMessageId;

        String knownId = redis.opsForValue().get(idemKey);
        if (knownId != null) {
            return messages.findById(knownId).map(m -> new SendResult(toDto(m), true))
                    .orElseGet(() -> insert(me, conversation, clientMessageId, content, idemKey));
        }
        return insert(me, conversation, clientMessageId, content, idemKey);
    }

    public void markRead(CurrentUser me, String conversationId) {
        requireParticipant(me, conversationId);
        mongo.updateFirst(Query.query(Criteria.where("_id").is(conversationId)),
                new Update().set("unread." + me.id(), 0), Conversation.class);
    }

    public int totalUnread(CurrentUser me) {
        return conversations.findByParticipantIdsContainsOrderByLastMessageAtDesc(me.id()).stream()
                .mapToInt(c -> c.getUnread().getOrDefault(me.id().toString(), 0))
                .sum();
    }

    public Conversation requireParticipant(CurrentUser me, String conversationId) {
        Conversation conversation = conversations.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));
        if (!conversation.hasParticipant(me.id())) {
            throw ApiException.forbidden("Not a participant in this conversation");
        }
        return conversation;
    }

    private SendResult insert(CurrentUser me, Conversation conversation, String clientMessageId, String content,
                              String idemKey) {
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderId(me.id());
        message.setSenderName(me.name());
        message.setContent(content.strip());
        message.setClientMessageId(clientMessageId);
        message.setCreatedAt(Instant.now());
        try {
            message = messages.insert(message);
        } catch (DuplicateKeyException duplicate) {
            ChatMessage existing = messages.findByConversationIdAndClientMessageId(conversation.getId(), clientMessageId)
                    .orElseThrow();
            redis.opsForValue().set(idemKey, existing.getId(), idempotencyTtl);
            return new SendResult(toDto(existing), true);
        }
        redis.opsForValue().set(idemKey, message.getId(), idempotencyTtl);

        Long recipient = conversation.otherParticipant(me.id());
        String preview = message.getContent().length() > 80 ? message.getContent().substring(0, 77) + "..."
                : message.getContent();
        mongo.updateFirst(Query.query(Criteria.where("_id").is(conversation.getId())),
                new Update().set("lastMessagePreview", preview)
                        .set("lastSenderId", me.id())
                        .set("lastMessageAt", message.getCreatedAt())
                        .inc("unread." + recipient, 1),
                Conversation.class);

        MessageDto dto = toDto(message);
        fanout.publish(conversation.getParticipantIds(), dto);
        events.publish(Topics.CHAT_MESSAGE, conversation.getId(), new ChatMessageEvent(UUID.randomUUID().toString(),
                conversation.getId(), message.getId(), me.id(), me.name(), recipient, preview, message.getCreatedAt()));
        return new SendResult(dto, false);
    }

    private static ConversationDto toDto(Conversation c, Long me) {
        Long other = c.otherParticipant(me);
        return new ConversationDto(c.getId(), other, c.getParticipantNames().get(other.toString()), c.getDealId(),
                c.getLastMessagePreview(), c.getLastSenderId(), c.getLastMessageAt(),
                c.getUnread().getOrDefault(me.toString(), 0));
    }

    static MessageDto toDto(ChatMessage m) {
        return new MessageDto(m.getId(), m.getConversationId(), m.getSenderId(), m.getSenderName(), m.getContent(),
                m.getClientMessageId(), m.getCreatedAt());
    }
}
