package com.loopers.infrastructure.eventhandled;

import com.loopers.domain.eventhandled.EventHandled;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventHandledJpaRepository extends JpaRepository<EventHandled, Long> {

    /**
     * 이벤트 ID로 처리 기록 조회
     */
    Optional<EventHandled> findByEventId(String eventId);

    /**
     * 이벤트 처리 여부 확인
     */
    boolean existsByEventId(String eventId);
}
