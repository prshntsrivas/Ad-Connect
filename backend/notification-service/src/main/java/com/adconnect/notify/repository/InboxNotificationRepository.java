package com.adconnect.notify.repository;

import com.adconnect.notify.document.InboxNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InboxNotificationRepository extends MongoRepository<InboxNotification, String> {

    Page<InboxNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadFalse(Long userId);
}
