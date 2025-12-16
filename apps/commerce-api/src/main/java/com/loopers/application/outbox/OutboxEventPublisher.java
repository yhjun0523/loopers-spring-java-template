package com.loopers.application.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.DomainEvent;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox Pattern 기반 이벤트 발행자
 * - 도메인 이벤트를 Outbox 테이블에 저장
 * - 별도 워커가 Outbox 테이블을 폴링하여 Kafka로 발행
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * 도메인 이벤트를 Outbox 테이블에 저장
     * - 도메인 변경과 동일한 트랜잭션에서 실행
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.create(
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                payload,
                event.getTopic()
            );

            outboxEventRepository.save(outboxEvent);

            log.info("Outbox 이벤트 저장 완료: eventType={}, aggregateId={}",
                event.getEventType(), event.getAggregateId());

        } catch (JsonProcessingException e) {
            log.error("이벤트 직렬화 실패: {}", event, e);
            throw new CoreException(ErrorType.INTERNAL_ERROR, "이벤트 직렬화 실패");
        }
    }
}
