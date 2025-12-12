package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("LikeService 통합 테스트")
class LikeServiceIntegrationTest {

    @Autowired
    private LikeService likeService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("좋아요 등록 시")
    @Nested
    class AddLike {

        @DisplayName("좋아요를 등록하면 Product의 likeCount가 증가한다")
        @Test
        @Transactional
        void addLike_increasesProductLikeCount() {
            // given
            Product product = Product.reconstitute(
                    null,
                    "테스트 상품",
                    "설명",
                    BigDecimal.valueOf(10000),
                    10,
                    "image.jpg",
                    1L,
                    ProductStatus.ACTIVE,
                    0,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            Product saved = productRepository.save(product);

            // when
            likeService.addLike("user1", saved.getId());

            // then
            Product updated = productRepository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getLikeCount()).isEqualTo(1);
            assertThat(likeRepository.existsByUserIdAndProductId("user1", saved.getId())).isTrue();
        }

        @DisplayName("이미 좋아요한 경우 멱등성을 보장한다")
        @Test
        @Transactional
        void addLike_idempotent() {
            // given
            Product product = Product.reconstitute(
                    null,
                    "테스트 상품",
                    "설명",
                    BigDecimal.valueOf(10000),
                    10,
                    "image.jpg",
                    1L,
                    ProductStatus.ACTIVE,
                    0,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            Product saved = productRepository.save(product);

            // when
            likeService.addLike("user1", saved.getId());
            likeService.addLike("user1", saved.getId()); // 중복 등록

            // then
            Product updated = productRepository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getLikeCount()).isEqualTo(1); // 한 번만 증가
        }
    }

    @DisplayName("좋아요 취소 시")
    @Nested
    class RemoveLike {

        @DisplayName("좋아요를 취소하면 Product의 likeCount가 감소한다")
        @Test
        @Transactional
        void removeLike_decreasesProductLikeCount() {
            // given
            Product product = Product.reconstitute(
                    null,
                    "테스트 상품",
                    "설명",
                    BigDecimal.valueOf(10000),
                    10,
                    "image.jpg",
                    1L,
                    ProductStatus.ACTIVE,
                    0,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            Product saved = productRepository.save(product);
            likeService.addLike("user1", saved.getId());

            // when
            likeService.removeLike("user1", saved.getId());

            // then
            Product updated = productRepository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getLikeCount()).isEqualTo(0);
            assertThat(likeRepository.existsByUserIdAndProductId("user1", saved.getId())).isFalse();
        }

        @DisplayName("이미 취소된 좋아요를 다시 취소해도 에러가 발생하지 않는다")
        @Test
        @Transactional
        void removeLike_idempotent() {
            // given
            Product product = Product.reconstitute(
                    null,
                    "테스트 상품",
                    "설명",
                    BigDecimal.valueOf(10000),
                    10,
                    "image.jpg",
                    1L,
                    ProductStatus.ACTIVE,
                    0,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            Product saved = productRepository.save(product);

            // when & then
            // 좋아요를 등록하지 않고 바로 취소해도 에러가 발생하지 않음
            likeService.removeLike("user1", saved.getId());

            Product updated = productRepository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getLikeCount()).isEqualTo(0);
        }
    }
}
