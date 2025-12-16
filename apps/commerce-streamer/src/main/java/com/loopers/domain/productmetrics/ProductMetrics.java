package com.loopers.domain.productmetrics;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 상품별 집계 데이터 엔티티
 * - 좋아요 수, 판매량, 조회 수 등의 메트릭스 저장
 * - Consumer가 이벤트를 받아 집계 처리 (Upsert)
 */
@Entity
@Table(
    name = "product_metrics",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_id", columnNames = "productId")
    },
    indexes = {
        @Index(name = "idx_like_count", columnList = "likeCount DESC"),
        @Index(name = "idx_sales_count", columnList = "salesCount DESC"),
        @Index(name = "idx_view_count", columnList = "viewCount DESC")
    }
)
public class ProductMetrics extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private Integer likeCount;

    @Column(nullable = false)
    private Integer salesCount;

    @Column(nullable = false)
    private Integer viewCount;

    @Version
    private Long version;

    protected ProductMetrics() {}

    private ProductMetrics(Long productId) {
        validateProductId(productId);
        this.productId = productId;
        this.likeCount = 0;
        this.salesCount = 0;
        this.viewCount = 0;
    }

    /**
     * ProductMetrics 생성 팩토리 메서드
     */
    public static ProductMetrics create(Long productId) {
        return new ProductMetrics(productId);
    }

    /**
     * 좋아요 수 증가
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 수 감소
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 판매량 증가
     */
    public void increaseSalesCount(int quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "판매량은 0보다 커야 합니다");
        }
        this.salesCount += quantity;
    }

    /**
     * 조회 수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    // Getters
    public Long getProductId() {
        return productId;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public Integer getSalesCount() {
        return salesCount;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public Long getVersion() {
        return version;
    }

    // 검증 메서드
    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 상품 ID입니다");
        }
    }
}
