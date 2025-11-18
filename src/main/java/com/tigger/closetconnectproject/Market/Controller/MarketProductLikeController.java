package com.tigger.closetconnectproject.Market.Controller;

import com.tigger.closetconnectproject.Market.Dto.MarketProductDtos;
import com.tigger.closetconnectproject.Market.Dto.MarketProductLikeDtos;
import com.tigger.closetconnectproject.Market.Service.MarketProductLikeService;
import com.tigger.closetconnectproject.Security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 중고거래 상품 찜/좋아요 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketProductLikeController {

    private final MarketProductLikeService likeService;

    /**
     * 찜 추가
     * POST /api/v1/market/products/{productId}/like
     */
    @PostMapping("/products/{productId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MarketProductLikeDtos.LikeStatusRes> addLike(
            @PathVariable Long productId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long userId = principal.getUser().getUserId();
        MarketProductLikeDtos.LikeStatusRes response = likeService.addLike(productId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 찜 취소
     * DELETE /api/v1/market/products/{productId}/like
     */
    @DeleteMapping("/products/{productId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MarketProductLikeDtos.LikeStatusRes> removeLike(
            @PathVariable Long productId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long userId = principal.getUser().getUserId();
        MarketProductLikeDtos.LikeStatusRes response = likeService.removeLike(productId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 찜 토글
     * PUT /api/v1/market/products/{productId}/like
     */
    @PutMapping("/products/{productId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MarketProductLikeDtos.LikeStatusRes> toggleLike(
            @PathVariable Long productId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long userId = principal.getUser().getUserId();
        MarketProductLikeDtos.LikeStatusRes response = likeService.toggleLike(productId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자가 찜한 상품 목록 조회
     * GET /api/v1/market/liked
     */
    @GetMapping("/liked")
    @PreAuthorize("isAuthenticated()")
    public Page<MarketProductDtos.ProductListRes> getUserLikedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long userId = principal.getUser().getUserId();
        return likeService.getUserLikedProducts(userId, page, size);
    }
}
