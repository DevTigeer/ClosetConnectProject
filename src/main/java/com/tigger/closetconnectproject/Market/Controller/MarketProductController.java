package com.tigger.closetconnectproject.Market.Controller;

import com.tigger.closetconnectproject.Market.Dto.MarketProductDtos;
import com.tigger.closetconnectproject.Market.Entity.ProductStatus;
import com.tigger.closetconnectproject.Market.Service.MarketProductLikeService;
import com.tigger.closetconnectproject.Market.Service.MarketProductService;
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
 * 중고거래 상품 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/market/products")
@RequiredArgsConstructor
public class MarketProductController {

    private final MarketProductService productService;
    private final MarketProductLikeService likeService;

    /**
     * 상품 목록 조회
     * GET /api/v1/market/products
     *
     * @param status   판매 상태 필터 (ON_SALE, RESERVED, SOLD)
     * @param region   지역 필터
     * @param keyword  검색 키워드
     * @param page     페이지 번호
     * @param size     페이지 크기
     * @param sort     정렬 (LATEST, PRICE_LOW, PRICE_HIGH)
     */
    @GetMapping
    public Page<MarketProductDtos.ProductListRes> list(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "LATEST") String sort,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long viewerId = (principal != null) ? principal.getUser().getUserId() : null;
        return productService.list(status, region, keyword, page, size, sort, viewerId);
    }

    /**
     * 상품 상세 조회
     * GET /api/v1/market/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MarketProductDtos.ProductDetailRes> getDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long viewerId = (principal != null) ? principal.getUser().getUserId() : null;

        // 조회수 증가 (별도 트랜잭션)
        productService.incrementViewCount(id);

        MarketProductDtos.ProductDetailRes response = productService.getProductDetail(id, viewerId);
        return ResponseEntity.ok(response);
    }

    /**
     * 상품 등록
     * POST /api/v1/market/products
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MarketProductDtos.ProductDetailRes> create(
            @Valid @RequestBody MarketProductDtos.CreateReq req,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long sellerId = principal.getUser().getUserId();
        MarketProductDtos.ProductDetailRes response = productService.create(sellerId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 상품 수정
     * PATCH /api/v1/market/products/{id}
     */
    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MarketProductDtos.ProductDetailRes> update(
            @PathVariable Long id,
            @Valid @RequestBody MarketProductDtos.UpdateReq req,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long userId = principal.getUser().getUserId();
        MarketProductDtos.ProductDetailRes response = productService.update(id, userId, req);
        return ResponseEntity.ok(response);
    }

    /**
     * 상품 상태 변경
     * PATCH /api/v1/market/products/{id}/status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MarketProductDtos.ProductDetailRes> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody MarketProductDtos.ChangeStatusReq req,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long userId = principal.getUser().getUserId();
        MarketProductDtos.ProductDetailRes response = productService.changeStatus(id, userId, req.getStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * 상품 삭제
     * DELETE /api/v1/market/products/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long userId = principal.getUser().getUserId();
        productService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 판매자별 상품 목록 조회
     * GET /api/v1/market/products/seller/{sellerId}
     */
    @GetMapping("/seller/{sellerId}")
    public Page<MarketProductDtos.ProductListRes> listBySeller(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long viewerId = (principal != null) ? principal.getUser().getUserId() : null;
        return productService.listBySeller(sellerId, page, size, viewerId);
    }
}
