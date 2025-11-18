package com.tigger.closetconnectproject.Market.Dto;

import lombok.*;

/**
 * 중고거래 상품 찜/좋아요 DTO 모음
 */
public class MarketProductLikeDtos {

    /**
     * 찜 상태 응답
     */
    @Getter @Builder
    public static class LikeStatusRes {
        private boolean liked;
        private Long likeCount;
    }
}
