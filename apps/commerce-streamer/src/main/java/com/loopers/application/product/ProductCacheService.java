package com.loopers.application.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

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
     * @param productId 상품 ID
     */
    public void evictProductDetail(Long productId) {
        String keyPattern = PRODUCT_DETAIL_KEY_PREFIX + productId + ":*";
        try {
            Set<String> keys = redisTemplate.keys(keyPattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("상품 상세 캐시 무효화 완료: keyPattern={}, count={}", keyPattern, keys.size());
            }
        } catch (Exception e) {
            log.error("상품 상세 캐시 무효화 실패: keyPattern={}, error={}", keyPattern, e.getMessage(), e);
        }
    }
}
