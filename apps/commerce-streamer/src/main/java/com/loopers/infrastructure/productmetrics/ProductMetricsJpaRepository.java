package com.loopers.infrastructure.productmetrics;

import com.loopers.domain.productmetrics.ProductMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, Long> {

    /**
     * 상품 ID로 메트릭스 조회
     */
    Optional<ProductMetrics> findByProductId(Long productId);
}
