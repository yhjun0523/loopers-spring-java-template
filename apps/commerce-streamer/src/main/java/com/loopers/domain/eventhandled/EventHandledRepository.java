package com.loopers.domain.eventhandled;

import java.util.Optional;

/**
 * 이벤트 처리 기록 레포지토리 인터페이스
 */
public interface EventHandledRepository {

    /**
     * 이벤트 처리 기록 저장
     */
    EventHandled save(EventHandled eventHandled);

    /**
     * 이벤트 ID로 처리 기록 조회 (중복 체크용)
     */
    Optional<EventHandled> findByEventId(String eventId);

    /**
     * 이벤트 처리 여부 확인
     */
    boolean existsByEventId(String eventId);
}
