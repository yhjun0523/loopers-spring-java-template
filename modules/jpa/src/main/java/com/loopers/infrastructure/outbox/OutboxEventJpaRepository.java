package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * 발행 대기 중인 이벤트를 생성 시각 순으로 조회
     */
    @Query("SELECT o FROM OutboxEvent o " +
           "WHERE o.status = :status " +
           "ORDER BY o.createdAt ASC " +
           "LIMIT :limit")
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(
        @Param("status") OutboxStatus status,
        @Param("limit") int limit
    );

    /**
     * 특정 시간 이전에 생성된 발행 완료 이벤트 조회
     */
    @Query("SELECT o FROM OutboxEvent o " +
           "WHERE o.status = :status " +
           "AND o.createdAt < :dateTime")
    List<OutboxEvent> findByStatusAndCreatedAtBefore(
        @Param("status") OutboxStatus status,
        @Param("dateTime") LocalDateTime dateTime
    );

    /**
     * 재시도 가능한 실패 이벤트 조회
     */
    @Query("SELECT o FROM OutboxEvent o " +
           "WHERE o.status = :status " +
           "AND o.retryCount < :maxRetry " +
           "ORDER BY o.createdAt ASC " +
           "LIMIT :limit")
    List<OutboxEvent> findRetryableFailedEvents(
        @Param("status") OutboxStatus status,
        @Param("maxRetry") int maxRetry,
        @Param("limit") int limit
    );
}
