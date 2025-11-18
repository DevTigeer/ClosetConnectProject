package com.tigger.closetconnectproject.Market.Service;

import com.tigger.closetconnectproject.Closet.Entity.Cloth;
import com.tigger.closetconnectproject.Closet.Repository.ClothRepository;
import com.tigger.closetconnectproject.Market.Dto.MarketProductDtos;
import com.tigger.closetconnectproject.Market.Entity.*;
import com.tigger.closetconnectproject.Market.Repository.MarketProductImageRepository;
import com.tigger.closetconnectproject.Market.Repository.MarketProductLikeRepository;
import com.tigger.closetconnectproject.Market.Repository.MarketProductRepository;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 중고거래 상품 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MarketProductService {

    private final MarketProductRepository productRepo;
    private final MarketProductImageRepository imageRepo;
    private final MarketProductLikeRepository likeRepo;
    private final ClothRepository clothRepo;
    private final UsersRepository userRepo;
    private final ChatService chatService;

    /**
     * 상품 등록
     */
    public MarketProductDtos.ProductDetailRes create(Long sellerId, MarketProductDtos.CreateReq req) {
        // 판매자 조회
        Users seller = userRepo.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

        // Cloth 조회 및 권한 확인
        Cloth cloth = clothRepo.findById(req.getClothId())
                .orElseThrow(() -> new IllegalArgumentException("옷장 아이템을 찾을 수 없습니다."));

        if (!cloth.getUser().getUserId().equals(sellerId)) {
            throw new AccessDeniedException("본인의 옷장 아이템만 판매할 수 있습니다.");
        }

        // 상품 생성
        MarketProduct product = MarketProduct.builder()
                .seller(seller)
                .cloth(cloth)
                .title(req.getTitle())
                .price(req.getPrice())
                .description(req.getDescription())
                .productCondition(req.getProductCondition())
                .region(req.getRegion())
                .brand(req.getBrand())
                .size(req.getSize())
                .gender(req.getGender())
                .status(ProductStatus.ON_SALE)
                .viewCount(0)
                .build();

        productRepo.save(product);

        // Cloth의 이미지를 대표 이미지로 추가
        if (cloth.getImageUrl() != null && !cloth.getImageUrl().isBlank()) {
            MarketProductImage mainImage = MarketProductImage.builder()
                    .marketProduct(product)
                    .imageUrl(cloth.getImageUrl())
                    .orderIndex(0)
                    .build();
            imageRepo.save(mainImage);
        }

        // 추가 이미지 등록
        if (req.getAdditionalImageUrls() != null && !req.getAdditionalImageUrls().isEmpty()) {
            int startIndex = 1;  // 0은 대표 이미지
            for (int i = 0; i < req.getAdditionalImageUrls().size(); i++) {
                MarketProductImage image = MarketProductImage.builder()
                        .marketProduct(product)
                        .imageUrl(req.getAdditionalImageUrls().get(i))
                        .orderIndex(startIndex + i)
                        .build();
                imageRepo.save(image);
            }
        }

        return getProductDetail(product.getId(), sellerId);
    }

    /**
     * 상품 목록 조회 (필터링 + 검색 + 정렬)
     */
    @Transactional(readOnly = true)
    public Page<MarketProductDtos.ProductListRes> list(
            ProductStatus status,
            String region,
            String keyword,
            int page,
            int size,
            String sortBy,
            Long viewerId
    ) {
        Sort sort = switch (sortBy != null ? sortBy.toUpperCase() : "LATEST") {
            case "PRICE_LOW" -> Sort.by(Sort.Direction.ASC, "price");
            case "PRICE_HIGH" -> Sort.by(Sort.Direction.DESC, "price");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 100), sort);

        Page<MarketProduct> products = productRepo.searchProducts(status, region, keyword, pageable);

        // 상품 ID 목록 추출
        List<Long> productIds = products.getContent().stream()
                .map(MarketProduct::getId)
                .collect(Collectors.toList());

        // 각 상품의 찜 개수 조회
        Map<Long, Long> likeCountMap = new HashMap<>();
        for (Long productId : productIds) {
            long count = likeRepo.countByMarketProduct_Id(productId);
            likeCountMap.put(productId, count);
        }

        // 각 상품의 대표 이미지 조회
        Map<Long, String> thumbnailMap = new HashMap<>();
        for (Long productId : productIds) {
            List<MarketProductImage> images = imageRepo.findByMarketProduct_IdOrderByOrderIndexAsc(productId);
            if (!images.isEmpty()) {
                thumbnailMap.put(productId, images.get(0).getImageUrl());
            }
        }

        return products.map(p -> MarketProductDtos.ProductListRes.of(
                p,
                thumbnailMap.get(p.getId()),
                likeCountMap.getOrDefault(p.getId(), 0L)
        ));
    }

    /**
     * 상품 상세 조회
     */
    @Transactional(readOnly = true)
    public MarketProductDtos.ProductDetailRes getProductDetail(Long productId, Long viewerId) {
        MarketProduct product = productRepo.findByIdWithDetails(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 조회수 증가는 트랜잭션 분리 필요 (읽기 전용이므로 여기서는 제외)
        // 실제로는 별도 메서드로 분리하거나, Controller에서 호출

        // 찜 개수 및 찜 여부 조회
        long likeCount = likeRepo.countByMarketProduct_Id(productId);
        boolean liked = viewerId != null && likeRepo.existsByMarketProduct_IdAndUser_UserId(productId, viewerId);

        // 이미지 목록 조회
        List<MarketProductImage> images = imageRepo.findByMarketProduct_IdOrderByOrderIndexAsc(productId);
        List<MarketProductDtos.ImageRes> imageResList = images.stream()
                .map(img -> MarketProductDtos.ImageRes.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .orderIndex(img.getOrderIndex())
                        .build())
                .collect(Collectors.toList());

        return MarketProductDtos.ProductDetailRes.of(product, likeCount, liked, imageResList);
    }

    /**
     * 조회수 증가 (별도 트랜잭션)
     */
    public void incrementViewCount(Long productId) {
        MarketProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        product.incrementViewCount();
    }

    /**
     * 상품 수정
     */
    public MarketProductDtos.ProductDetailRes update(Long productId, Long userId, MarketProductDtos.UpdateReq req) {
        MarketProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 권한 확인
        if (!product.isSeller(userId)) {
            throw new AccessDeniedException("본인의 상품만 수정할 수 있습니다.");
        }

        // 상품 정보 업데이트
        product.update(
                req.getTitle(),
                req.getPrice(),
                req.getDescription(),
                req.getProductCondition(),
                req.getRegion(),
                req.getBrand(),
                req.getSize(),
                req.getGender()
        );

        return getProductDetail(productId, userId);
    }

    /**
     * 상품 상태 변경 (판매중 -> 예약중 -> 거래완료)
     */
    public MarketProductDtos.ProductDetailRes changeStatus(Long productId, Long userId, ProductStatus newStatus) {
        MarketProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 권한 확인
        if (!product.isSeller(userId)) {
            throw new AccessDeniedException("본인의 상품만 상태를 변경할 수 있습니다.");
        }

        ProductStatus oldStatus = product.getStatus();
        product.changeStatus(newStatus);

        // 상태 변경 시스템 메시지 전송
        if (!oldStatus.equals(newStatus)) {
            String statusText = switch (newStatus) {
                case ON_SALE -> "판매중";
                case RESERVED -> "예약중";
                case SOLD -> "거래완료";
            };
            chatService.sendSystemMessage(productId, "판매자가 상품 상태를 '" + statusText + "'로 변경했습니다.");
        }

        return getProductDetail(productId, userId);
    }

    /**
     * 상품 삭제
     */
    public void delete(Long productId, Long userId) {
        MarketProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 권한 확인
        if (!product.isSeller(userId)) {
            throw new AccessDeniedException("본인의 상품만 삭제할 수 있습니다.");
        }

        // 이미지 먼저 삭제
        imageRepo.deleteByMarketProduct_Id(productId);

        // 상품 삭제
        productRepo.delete(product);
    }

    /**
     * 판매자별 상품 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<MarketProductDtos.ProductListRes> listBySeller(Long sellerId, int page, int size, Long viewerId) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<MarketProduct> products = productRepo.findBySeller_UserId(sellerId, pageable);

        // 찜 개수 및 썸네일 조회
        Map<Long, Long> likeCountMap = new HashMap<>();
        Map<Long, String> thumbnailMap = new HashMap<>();

        for (MarketProduct p : products.getContent()) {
            long count = likeRepo.countByMarketProduct_Id(p.getId());
            likeCountMap.put(p.getId(), count);

            List<MarketProductImage> images = imageRepo.findByMarketProduct_IdOrderByOrderIndexAsc(p.getId());
            if (!images.isEmpty()) {
                thumbnailMap.put(p.getId(), images.get(0).getImageUrl());
            }
        }

        return products.map(p -> MarketProductDtos.ProductListRes.of(
                p,
                thumbnailMap.get(p.getId()),
                likeCountMap.getOrDefault(p.getId(), 0L)
        ));
    }

    /**
     * 상품 Entity 조회 (내부 사용)
     */
    @Transactional(readOnly = true)
    public MarketProduct getProductEntity(Long productId) {
        return productRepo.findByIdWithDetails(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
    }
}
