package com.adconnect.chat.controller;

import com.adconnect.chat.dto.ChatDtos.ConversationDto;
import com.adconnect.chat.dto.ChatDtos.MessageDto;
import com.adconnect.chat.dto.ChatDtos.SendMessageRequest;
import com.adconnect.chat.dto.ChatDtos.SendResult;
import com.adconnect.chat.dto.ChatDtos.StartConversationRequest;
import com.adconnect.chat.dto.ChatDtos.UnreadDto;
import com.adconnect.chat.service.ChatService;
import com.adconnect.common.security.AuthUser;
import com.adconnect.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Tag(name = "Chat")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chat;

    @PostMapping("/conversations")
    public ConversationDto start(@AuthUser CurrentUser me, @Valid @RequestBody StartConversationRequest request) {
        return chat.startConversation(me, request.participantId(), request.dealId());
    }

    @GetMapping("/conversations")
    public List<ConversationDto> list(@AuthUser CurrentUser me) {
        return chat.myConversations(me);
    }

    @GetMapping("/conversations/{id}/messages")
    public List<MessageDto> history(@AuthUser CurrentUser me, @PathVariable String id,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
                                    @RequestParam(defaultValue = "50") int limit) {
        return chat.history(me, id, before, limit);
    }

    @Operation(summary = "Send a message. Retrying with the same clientMessageId returns the original (200) instead of creating a duplicate (201).")
    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<MessageDto> send(@AuthUser CurrentUser me, @PathVariable String id,
                                           @Valid @RequestBody SendMessageRequest request) {
        SendResult result = chat.send(me, id, request.clientMessageId(), request.content());
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header("Idempotent-Replay", String.valueOf(result.replayed()))
                .body(result.message());
    }

    @PostMapping("/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@AuthUser CurrentUser me, @PathVariable String id) {
        chat.markRead(me, id);
    }

    @GetMapping("/unread")
    public UnreadDto unread(@AuthUser CurrentUser me) {
        return new UnreadDto(chat.totalUnread(me));
    }
}
