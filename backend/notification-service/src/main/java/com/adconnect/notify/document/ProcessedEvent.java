package com.adconnect.notify.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Dedupes Kafka retries / DLT replays by eventId. */
@Document("processed_events")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedEvent {

    @Id
    private String eventId;

    private String topic;

    private Instant processedAt;

    public ProcessedEvent(String eventId, String topic) {
        this.eventId = eventId;
        this.topic = topic;
        this.processedAt = Instant.now();
    }
}
