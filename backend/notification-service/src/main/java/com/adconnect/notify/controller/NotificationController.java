package com.adconnect.notify.controller;

import com.adconnect.common.security.AuthUser;
import com.adconnect.common.security.CurrentUser;
import com.adconnect.common.web.ApiException;
import com.adconnect.common.web.PageResponse;
import com.adconnect.notify.document.InboxNotification;
import com.adconnect.notify.repository.InboxNotificationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final InboxNotificationRepository inbox;

    @GetMapping
    public PageResponse<InboxNotification> list(@AuthUser CurrentUser user,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(inbox.findByUserIdOrderByCreatedAtDesc(user.id(),
                PageRequest.of(page, Math.min(size, 50))), n -> n);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@AuthUser CurrentUser user) {
        return Map.of("count", inbox.countByUserIdAndReadFalse(user.id()));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@AuthUser CurrentUser user, @PathVariable String id) {
        InboxNotification n = inbox.findById(id).orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUserId().equals(user.id())) {
            throw ApiException.forbidden("Not your notification");
        }
        n.setRead(true);
        inbox.save(n);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(@AuthUser CurrentUser user) {
        inbox.findByUserIdOrderByCreatedAtDesc(user.id(), PageRequest.of(0, 500))
                .forEach(n -> {
                    n.setRead(true);
                    inbox.save(n);
                });
    }
}
