package com.tigger.closetconnectproject.Market.Controller;

import com.tigger.closetconnectproject.Market.Dto.MarketProductCommentDtos;
import com.tigger.closetconnectproject.Market.Service.MarketProductCommentService;
import com.tigger.closetconnectproject.Security.AppUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 중고거래 상품 댓글 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/market/products/{productId}/comments")
@RequiredArgsConstructor
public class MarketProductCommentController {

    private final MarketProductCommentService commentService;

    /**
     * 댓글 목록 조회
     * GET /api/v1/market/products/{productId}/comments
     */
    @GetMapping
    public Page<MarketProductCommentDtos.CommentRes> list(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return commentService.list(productId, page, size);
    }

    /**
     * 댓글 작성
     * POST /api/v1/market/products/{productId}/comments
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MarketProductCommentDtos.CommentRes> create(
            @PathVariable Long productId,
            @Valid @RequestBody MarketProductCommentDtos.CreateReq req,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long userId = principal.getUser().getUserId();
        MarketProductCommentDtos.CommentRes response = commentService.create(productId, userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 댓글 수정
     * PATCH /api/v1/market/products/{productId}/comments/{commentId}
     */
    @PatchMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MarketProductCommentDtos.CommentRes> update(
            @PathVariable Long productId,
            @PathVariable Long commentId,
            @Valid @RequestBody MarketProductCommentDtos.UpdateReq req,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long userId = principal.getUser().getUserId();
        MarketProductCommentDtos.CommentRes response = commentService.update(commentId, userId, req);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 삭제
     * DELETE /api/v1/market/products/{productId}/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable Long productId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long userId = principal.getUser().getUserId();
        commentService.delete(commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
