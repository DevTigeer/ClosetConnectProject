package com.tigger.closetconnectproject.Market.Entity;

import com.tigger.closetconnectproject.Common.Entity.BaseTimeEntity;
import com.tigger.closetconnectproject.User.Entity.Users;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;

/**
 * 중고거래 상품 댓글 엔티티
 */
@Entity
@Table(name = "market_product_comment",
        indexes = @Index(name = "idx_comment_product_created", columnList = "market_product_id, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MarketProductComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상품 참조
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "market_product_id", nullable = false)
    private MarketProduct marketProduct;

    // 작성자
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Users author;

    // 부모 댓글 (대댓글인 경우)
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "parent_id")
    private MarketProductComment parent;

    // 댓글 내용
    @Lob
    @Column(nullable = false)
    private String content;

    // 댓글 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CommentStatus status = CommentStatus.ACTIVE;

    /**
     * 댓글 수정
     */
    public void edit(String content) {
        if (content != null) {
            this.content = content;
        }
    }

    /**
     * 댓글 소프트 삭제
     */
    public void softDelete() {
        this.status = CommentStatus.DELETED;
    }

    /**
     * 작성자 확인
     */
    public boolean isAuthor(Long userId) {
        return this.author.getUserId().equals(userId);
    }

    /**
     * 판매자 댓글인지 확인
     */
    public boolean isSellerComment() {
        return this.author.getUserId().equals(this.marketProduct.getSeller().getUserId());
    }
}
