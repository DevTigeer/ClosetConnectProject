package com.tigger.closetconnectproject.Market.Dto;

import com.tigger.closetconnectproject.Market.Entity.MarketProductComment;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 중고거래 상품 댓글 DTO 모음
 */
public class MarketProductCommentDtos {

    /**
     * 댓글 작성 요청
     */
    @Getter @Setter @NoArgsConstructor
    public static class CreateReq {
        @NotBlank
        private String content;

        private Long parentId;  // 대댓글인 경우 부모 댓글 ID
    }

    /**
     * 댓글 수정 요청
     */
    @Getter @Setter @NoArgsConstructor
    public static class UpdateReq {
        @NotBlank
        private String content;
    }

    /**
     * 댓글 응답
     */
    @Getter @Builder
    public static class CommentRes {
        private Long id;
        private Long marketProductId;
        private Long parentId;
        private String content;
        private String authorName;
        private Long authorId;
        private boolean isSellerComment;  // 판매자 댓글인지 여부
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static CommentRes of(MarketProductComment comment) {
            return CommentRes.builder()
                    .id(comment.getId())
                    .marketProductId(comment.getMarketProduct().getId())
                    .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                    .content(comment.getContent())
                    .authorName(comment.getAuthor().getNickname())
                    .authorId(comment.getAuthor().getUserId())
                    .isSellerComment(comment.isSellerComment())
                    .createdAt(comment.getCreatedAt())
                    .updatedAt(comment.getUpdatedAt())
                    .build();
        }
    }
}
