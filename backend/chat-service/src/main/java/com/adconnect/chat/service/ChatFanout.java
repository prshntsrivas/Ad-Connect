package com.adconnect.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

/**
 * Cross-instance delivery: every chat-service instance subscribes to one Redis channel and pushes
 * frames to the WebSocket sessions it holds locally, so horizontally scaled instances behind the
 * load balancer still reach every connected participant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatFanout implements MessageListener {

    public static final String CHANNEL = "adconnect:chat:fanout";

    private final StringRedisTemplate redis;
    private final SimpMessagingTemplate messaging;
    private final ObjectMapper objectMapper;

    public void publish(Collection<Long> recipients, Object message) {
        send(recipients, "/queue/messages", message);
    }

    public void publishTyping(Collection<Long> recipients, Object typing) {
        send(recipients, "/queue/typing", typing);
    }

    private void send(Collection<Long> recipients, String destination, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "recipients", recipients, "destination", destination, "payload", payload));
            redis.convertAndSend(CHANNEL, json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        } catch (RuntimeException redisDown) {
            log.warn("Redis fan-out unavailable, delivering locally only: {}", redisDown.getMessage());
            recipients.forEach(r -> messaging.convertAndSendToUser(r.toString(), destination, payload));
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            JsonNode node = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
            String destination = node.get("destination").asText();
            Object payload = objectMapper.treeToValue(node.get("payload"), Object.class);
            node.get("recipients").forEach(r -> messaging.convertAndSendToUser(r.asText(), destination, payload));
        } catch (Exception e) {
            log.error("Bad fan-out frame: {}", e.getMessage());
        }
    }
}
