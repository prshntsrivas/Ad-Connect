package com.adconnect.chat.controller;

import com.adconnect.chat.config.StompPrincipal;
import com.adconnect.chat.document.Conversation;
import com.adconnect.chat.dto.ChatDtos.TypingEvent;
import com.adconnect.chat.dto.ChatDtos.WsSendMessage;
import com.adconnect.chat.dto.ChatDtos.WsTyping;
import com.adconnect.chat.service.ChatFanout;
import com.adconnect.chat.service.ChatService;
import com.adconnect.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final ChatService chat;
    private final ChatFanout fanout;

    /** STOMP SEND /app/chat.send — delivery to both participants happens via the fan-out. */
    @MessageMapping("/chat.send")
    public void send(@Payload WsSendMessage message, Principal principal) {
        chat.send(user(principal), message.conversationId(), message.clientMessageId(), message.content());
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload WsTyping typing, Principal principal) {
        CurrentUser me = user(principal);
        Conversation conversation = chat.requireParticipant(me, typing.conversationId());
        fanout.publishTyping(List.of(conversation.otherParticipant(me.id())),
                new TypingEvent(conversation.getId(), me.id(), me.name()));
    }

    @MessageExceptionHandler
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public Map<String, String> onError(Exception e) {
        return Map.of("error", e.getMessage() == null ? "Unexpected error" : e.getMessage());
    }

    private static CurrentUser user(Principal principal) {
        return ((StompPrincipal) principal).user();
    }
}
