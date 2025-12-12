package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 좋아요 도메인 서비스
 * - 좋아요 등록/취소 비즈니스 로직
 * - Product의 likeCount 동기화
 * - 멱등성 보장
 * - 트랜잭션 경계는 Application Layer에서 관리한다.
 */
@RequiredArgsConstructor
@Component
public class LikeService {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;

    /**
     * 좋아요 등록
     * - 멱등성 보장: 이미 좋아요한 경우 무시
     * - Product의 likeCount 증가
     */
    public void addLike(String userId, Long productId) {
        // 이미 존재하면 무시 (멱등성)
        if (likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }

        // 좋아요 저장
        Like like = Like.create(userId, productId);
        likeRepository.save(like);

        // Product의 likeCount 증가
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: " + productId));
        product.incrementLikeCount();
        productRepository.save(product);
    }

    /**
     * 좋아요 취소
     * - 멱등성 보장: 이미 취소된 경우에도 에러 없음
     * - Product의 likeCount 감소
     */
    public void removeLike(String userId, Long productId) {
        // 좋아요가 존재하는지 확인
        boolean existed = likeRepository.existsByUserIdAndProductId(userId, productId);

        // 좋아요 삭제
        likeRepository.deleteByUserIdAndProductId(userId, productId);

        // 좋아요가 존재했던 경우에만 likeCount 감소
        if (existed) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: " + productId));
            product.decrementLikeCount();
            productRepository.save(product);
        }
    }

    /**
     * 좋아요 여부 확인
     */
    public boolean isLiked(String userId, Long productId) {
        return likeRepository.existsByUserIdAndProductId(userId, productId);
    }

    /**
     * 상품의 좋아요 수 조회
     */
    public int getLikeCount(Long productId) {
        return likeRepository.countByProductId(productId);
    }

    /**
     * 사용자가 좋아요한 상품 ID 목록 조회
     */
    public List<Long> getLikedProductIds(String userId) {
        return likeRepository.findProductIdsByUserId(userId);
    }
}
