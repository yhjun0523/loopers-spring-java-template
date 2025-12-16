package com.loopers.domain.outbox;

/**
 * Outbox 이벤트의 발행 상태
 */
public enum OutboxStatus {
    /**
     * 발행 대기 중
     */
    PENDING,

    /**
     * 발행 완료
     */
    PUBLISHED,

    /**
     * 발행 실패
     */
    FAILED
}
