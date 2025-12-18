package com.loopers.domain.outbox;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 이벤트 레포지토리 인터페이스
 */
public interface OutboxEventRepository {

    /**
     * Outbox 이벤트 저장
     */
    OutboxEvent save(OutboxEvent outboxEvent);

    /**
     * 발행 대기 중인 이벤트 조회
     */
    List<OutboxEvent> findPendingEvents(int limit);

    /**
     * 특정 시간 이전에 생성된 발행 완료 이벤트 조회 (정리용)
     */
    List<OutboxEvent> findPublishedEventsBefore(LocalDateTime dateTime);

    /**
     * 재시도 가능한 실패 이벤트 조회
     */
    List<OutboxEvent> findRetryableFailedEvents(int limit);
}
