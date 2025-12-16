package com.loopers.application.productmetrics;

import com.loopers.domain.productmetrics.ProductMetrics;
import com.loopers.domain.productmetrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 메트릭스 집계 서비스
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ProductMetricsService {

    private final ProductMetricsRepository productMetricsRepository;

    /**
     * 좋아요 수 증가
     */
    @Transactional
    public void incrementLikeCount(Long productId) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
            .orElseGet(() -> ProductMetrics.create(productId));

        metrics.incrementLikeCount();
        productMetricsRepository.save(metrics);

        log.info("좋아요 수 증가 완료: productId={}, likeCount={}", productId, metrics.getLikeCount());
    }

    /**
     * 좋아요 수 감소
     */
    @Transactional
    public void decrementLikeCount(Long productId) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
            .orElseGet(() -> ProductMetrics.create(productId));

        metrics.decrementLikeCount();
        productMetricsRepository.save(metrics);

        log.info("좋아요 수 감소 완료: productId={}, likeCount={}", productId, metrics.getLikeCount());
    }

    /**
     * 판매량 증가
     */
    @Transactional
    public void increaseSalesCount(Long productId, int quantity) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
            .orElseGet(() -> ProductMetrics.create(productId));

        metrics.increaseSalesCount(quantity);
        productMetricsRepository.save(metrics);

        log.info("판매량 증가 완료: productId={}, quantity={}, salesCount={}",
            productId, quantity, metrics.getSalesCount());
    }

    /**
     * 조회 수 증가
     */
    @Transactional
    public void incrementViewCount(Long productId) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
            .orElseGet(() -> ProductMetrics.create(productId));

        metrics.incrementViewCount();
        productMetricsRepository.save(metrics);

        log.info("조회 수 증가 완료: productId={}, viewCount={}", productId, metrics.getViewCount());
    }
}
