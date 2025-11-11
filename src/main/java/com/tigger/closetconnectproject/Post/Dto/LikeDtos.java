package com.tigger.closetconnectproject.Post.Dto;

import lombok.Builder;

public class LikeDtos {
    @Builder
    public record LikeStatusRes(
            boolean liked,  // 현재 사용자 기준
            long count      // 총 좋아요 수
    ) {}
}
