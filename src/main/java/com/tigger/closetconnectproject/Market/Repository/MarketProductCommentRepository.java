package com.tigger.closetconnectproject.Market.Repository;

import com.tigger.closetconnectproject.Market.Entity.CommentStatus;
import com.tigger.closetconnectproject.Market.Entity.MarketProductComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 중고거래 상품 댓글 Repository
 */
public interface MarketProductCommentRepository extends JpaRepository<MarketProductComment, Long> {

    /**
     * 상품의 댓글 목록 조회 (활성 상태만, 생성일 순)
     */
    Page<MarketProductComment> findByMarketProduct_IdAndStatusOrderByCreatedAtAsc(
            Long marketProductId,
            CommentStatus status,
            Pageable pageable
    );

    /**
     * 상품의 댓글 목록 조회 (모든 상태, 생성일 순)
     */
    List<MarketProductComment> findByMarketProduct_IdOrderByCreatedAtAsc(Long marketProductId);

    /**
     * 상품의 활성 댓글 개수
     */
    long countByMarketProduct_IdAndStatus(Long marketProductId, CommentStatus status);
}
