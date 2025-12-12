package com.loopers.application.like;

import com.loopers.domain.like.LikeService;
import com.loopers.infrastructure.cache.ProductCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("LikeFacade 단위 테스트")
class LikeFacadeTest {

    @Test
    @DisplayName("addLike는 도메인 서비스에 위임하고 캐시를 무효화한다 (멱등)")
    void addLike_delegates() {
        // given
        LikeService likeService = mock(LikeService.class);
        ProductCacheService cacheService = mock(ProductCacheService.class);
        LikeFacade facade = new LikeFacade(likeService, cacheService);

        // when
        facade.addLike("user-1", 10L);

        // then
        verify(likeService, times(1)).addLike("user-1", 10L);
        verify(cacheService, times(1)).evictProductDetail(10L);
        verify(cacheService, times(1)).evictProductList();
    }

    @Test
    @DisplayName("removeLike는 도메인 서비스에 위임하고 캐시를 무효화한다 (멱등)")
    void removeLike_delegates() {
        // given
        LikeService likeService = mock(LikeService.class);
        ProductCacheService cacheService = mock(ProductCacheService.class);
        LikeFacade facade = new LikeFacade(likeService, cacheService);

        // when
        facade.removeLike("user-1", 10L);

        // then
        verify(likeService, times(1)).removeLike("user-1", 10L);
        verify(cacheService, times(1)).evictProductDetail(10L);
        verify(cacheService, times(1)).evictProductList();
    }

    @Test
    @DisplayName("isLiked는 결과를 그대로 반환한다")
    void isLiked_returns() {
        // given
        LikeService likeService = mock(LikeService.class);
        ProductCacheService cacheService = mock(ProductCacheService.class);
        when(likeService.isLiked("u", 1L)).thenReturn(true);
        LikeFacade facade = new LikeFacade(likeService, cacheService);

        // when
        boolean liked = facade.isLiked("u", 1L);

        // then
        assertThat(liked).isTrue();
        verify(likeService, times(1)).isLiked("u", 1L);
        verifyNoMoreInteractions(likeService);
    }

    @Test
    @DisplayName("getLikeCount는 결과를 그대로 반환한다")
    void getLikeCount_returns() {
        // given
        LikeService likeService = mock(LikeService.class);
        ProductCacheService cacheService = mock(ProductCacheService.class);
        when(likeService.getLikeCount(1L)).thenReturn(7);
        LikeFacade facade = new LikeFacade(likeService, cacheService);

        // when
        int count = facade.getLikeCount(1L);

        // then
        assertThat(count).isEqualTo(7);
        verify(likeService, times(1)).getLikeCount(1L);
        verifyNoMoreInteractions(likeService);
    }

    @Test
    @DisplayName("getLikedProductIds는 결과를 그대로 반환한다")
    void getLikedProductIds_returns() {
        // given
        LikeService likeService = mock(LikeService.class);
        ProductCacheService cacheService = mock(ProductCacheService.class);
        when(likeService.getLikedProductIds("u")).thenReturn(List.of(1L, 2L, 3L));
        LikeFacade facade = new LikeFacade(likeService, cacheService);

        // when
        List<Long> ids = facade.getLikedProductIds("u");

        // then
        assertThat(ids).containsExactly(1L, 2L, 3L);
        verify(likeService, times(1)).getLikedProductIds("u");
        verifyNoMoreInteractions(likeService);
    }
}
