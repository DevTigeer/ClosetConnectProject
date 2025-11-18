package com.tigger.closetconnectproject.Market.Service;

import com.tigger.closetconnectproject.Market.Dto.MarketProductCommentDtos;
import com.tigger.closetconnectproject.Market.Entity.CommentStatus;
import com.tigger.closetconnectproject.Market.Entity.MarketProduct;
import com.tigger.closetconnectproject.Market.Entity.MarketProductComment;
import com.tigger.closetconnectproject.Market.Repository.MarketProductCommentRepository;
import com.tigger.closetconnectproject.Market.Repository.MarketProductRepository;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 중고거래 상품 댓글 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MarketProductCommentService {

    private final MarketProductCommentRepository commentRepo;
    private final MarketProductRepository productRepo;
    private final UsersRepository userRepo;

    /**
     * 댓글 작성
     */
    public MarketProductCommentDtos.CommentRes create(Long productId, Long userId, MarketProductCommentDtos.CreateReq req) {
        // 상품 조회
        MarketProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 사용자 조회
        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 부모 댓글 조회 (대댓글인 경우)
        MarketProductComment parent = null;
        if (req.getParentId() != null) {
            parent = commentRepo.findById(req.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));

            // 부모 댓글이 동일한 상품의 댓글인지 확인
            if (!parent.getMarketProduct().getId().equals(productId)) {
                throw new IllegalArgumentException("잘못된 부모 댓글입니다.");
            }
        }

        // 댓글 생성
        MarketProductComment comment = MarketProductComment.builder()
                .marketProduct(product)
                .author(user)
                .parent(parent)
                .content(req.getContent())
                .status(CommentStatus.ACTIVE)
                .build();

        commentRepo.save(comment);

        return MarketProductCommentDtos.CommentRes.of(comment);
    }

    /**
     * 댓글 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<MarketProductCommentDtos.CommentRes> list(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 100));

        Page<MarketProductComment> comments = commentRepo.findByMarketProduct_IdAndStatusOrderByCreatedAtAsc(
                productId,
                CommentStatus.ACTIVE,
                pageable
        );

        return comments.map(MarketProductCommentDtos.CommentRes::of);
    }

    /**
     * 댓글 수정
     */
    public MarketProductCommentDtos.CommentRes update(Long commentId, Long userId, MarketProductCommentDtos.UpdateReq req) {
        MarketProductComment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 권한 확인
        if (!comment.isAuthor(userId)) {
            throw new AccessDeniedException("본인의 댓글만 수정할 수 있습니다.");
        }

        // 댓글 수정
        comment.edit(req.getContent());

        return MarketProductCommentDtos.CommentRes.of(comment);
    }

    /**
     * 댓글 삭제 (소프트 삭제)
     */
    public void delete(Long commentId, Long userId) {
        MarketProductComment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 권한 확인
        if (!comment.isAuthor(userId)) {
            throw new AccessDeniedException("본인의 댓글만 삭제할 수 있습니다.");
        }

        // 소프트 삭제
        comment.softDelete();
    }
}
