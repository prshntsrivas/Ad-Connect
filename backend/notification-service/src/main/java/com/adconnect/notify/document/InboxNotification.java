package com.adconnect.notify.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("notifications")
@CompoundIndex(name = "uk_user_event", def = "{'userId': 1, 'eventId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
public class InboxNotification {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private String eventId;

    private String type;

    private String title;

    private String body;

    private String link;

    private boolean read;

    private Instant createdAt;
}
