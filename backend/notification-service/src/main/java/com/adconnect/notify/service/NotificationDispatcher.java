package com.adconnect.notify.service;

import com.adconnect.notify.config.MailProperties;
import com.adconnect.notify.document.InboxNotification;
import com.adconnect.notify.document.ProcessedEvent;
import com.adconnect.notify.repository.InboxNotificationRepository;
import com.adconnect.notify.repository.ProcessedEventRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final ProcessedEventRepository processed;
    private final InboxNotificationRepository inbox;
    private final JavaMailSender mail;
    private final SpringTemplateEngine templates;
    private final MailProperties properties;

    /**
     * @return false if this eventId was already handled (idempotent skip)
     */
    public boolean claim(String eventId, String topic) {
        if (eventId == null) {
            return true;
        }
        if (processed.existsById(eventId)) {
            return false;
        }
        try {
            processed.save(new ProcessedEvent(eventId, topic));
            return true;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    public void notifyUser(Long userId, String eventId, String type, String title, String body, String link,
                           String email, String template, Map<String, Object> model) {
        InboxNotification n = new InboxNotification();
        n.setUserId(userId);
        n.setEventId(eventId);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setLink(link);
        n.setCreatedAt(Instant.now());
        try {
            inbox.save(n);
        } catch (DuplicateKeyException ignored) {
            return;
        }
        if (email != null && !email.isBlank()) {
            sendMail(email, title, template, model);
        }
    }

    public void sendMail(String to, String subject, String template, Map<String, Object> model) {
        try {
            Context ctx = new Context();
            ctx.setVariable("frontendUrl", properties.frontendUrl());
            if (model != null) {
                model.forEach(ctx::setVariable);
            }
            String html = templates.process(template, ctx);
            MimeMessage message = mail.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mail.send(message);
            log.info("Mailed '{}' to {}", subject, to);
        } catch (Exception e) {
            log.error("Failed to mail {} ({}): {}", to, subject, e.getMessage());
            throw new IllegalStateException("Mail send failed", e);
        }
    }
}
