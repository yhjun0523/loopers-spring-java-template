package com.loopers.domain.productmetrics;

import java.util.Optional;

/**
 * 상품 메트릭스 레포지토리 인터페이스
 */
public interface ProductMetricsRepository {

    /**
     * ProductMetrics 저장
     */
    ProductMetrics save(ProductMetrics productMetrics);

    /**
     * 상품 ID로 메트릭스 조회
     */
    Optional<ProductMetrics> findByProductId(Long productId);
}
