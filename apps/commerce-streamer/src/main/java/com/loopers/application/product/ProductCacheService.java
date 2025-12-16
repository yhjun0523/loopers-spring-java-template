package com.loopers.application.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 상품 캐시 서비스 (Streamer)
 * - commerce-api에서 관리하는 상품 캐시를 무효화
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ProductCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PRODUCT_DETAIL_KEY_PREFIX = "product:detail:";

    /**
     * 상품 상세 캐시를 무효화한다.
     * - 특정 상품의 모든 사용자별 캐시를 삭제
     * - SCAN 명령어를 사용하여 Redis 성능에 영향을 최소화
     * @param productId 상품 ID
     */
    public void evictProductDetail(Long productId) {
        String keyPattern = PRODUCT_DETAIL_KEY_PREFIX + productId + ":*";
        try {
            List<String> keysToDelete = new ArrayList<>();

            // SCAN 명령어로 패턴 매칭되는 키 탐색 (페이징 방식)
            ScanOptions options = ScanOptions.scanOptions()
                .match(keyPattern)
                .count(100) // 한 번에 스캔할 키 개수
                .build();

            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keysToDelete.add(cursor.next());
                }
            }

            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
                log.info("상품 상세 캐시 무효화 완료: keyPattern={}, count={}", keyPattern, keysToDelete.size());
            }
        } catch (Exception e) {
            log.error("상품 상세 캐시 무효화 실패: keyPattern={}, error={}", keyPattern, e.getMessage(), e);
        }
    }
}
