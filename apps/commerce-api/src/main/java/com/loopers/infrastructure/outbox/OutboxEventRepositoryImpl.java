package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import com.loopers.domain.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        return outboxEventJpaRepository.save(outboxEvent);
    }

    @Override
    public List<OutboxEvent> findPendingEvents(int limit) {
        return outboxEventJpaRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, limit);
    }

    @Override
    public List<OutboxEvent> findPublishedEventsBefore(LocalDateTime dateTime) {
        return outboxEventJpaRepository.findByStatusAndCreatedAtBefore(OutboxStatus.PUBLISHED, dateTime);
    }

    @Override
    public List<OutboxEvent> findRetryableFailedEvents(int limit) {
        return outboxEventJpaRepository.findRetryableFailedEvents(OutboxStatus.FAILED, 5, limit);
    }
}
