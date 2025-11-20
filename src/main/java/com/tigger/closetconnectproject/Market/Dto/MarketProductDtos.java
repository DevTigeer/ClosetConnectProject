package com.tigger.closetconnectproject.Market.Dto;

import com.tigger.closetconnectproject.Market.Entity.MarketProduct;
import com.tigger.closetconnectproject.Market.Entity.ProductCondition;
import com.tigger.closetconnectproject.Market.Entity.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 중고거래 상품 DTO 모음
 */
public class MarketProductDtos {

    /**
     * 상품 등록 요청
     */
    @Getter @Setter @NoArgsConstructor
    public static class CreateReq {
        @NotNull
        private Long clothId;  // 옷장 아이템 ID

        @NotBlank
        private String title;

        @NotNull
        @Min(0)
        private Integer price;

        @NotBlank
        private String description;

        @NotNull
        private ProductCondition productCondition;

        private String region;  // 거래 지역 (선택)
        private String brand;   // 브랜드 (선택)
        private String size;    // 사이즈 (선택)
        private String gender;  // 성별 (선택)

        // 추가 이미지 URL 리스트 (Cloth의 이미지 외 추가 이미지)
        private List<String> additionalImageUrls;
    }

    /**
     * 상품 수정 요청
     */
    @Getter @Setter @NoArgsConstructor
    public static class UpdateReq {
        private String title;
        private Integer price;
        private String description;
        private ProductCondition productCondition;
        private String region;
        private String brand;
        private String size;
        private String gender;
    }

    /**
     * 상품 상태 변경 요청
     */
    @Getter @Setter @NoArgsConstructor
    public static class ChangeStatusReq {
        @NotNull
        private ProductStatus status;
    }

    /**
     * 상품 이미지 응답
     */
    @Getter @Builder
    public static class ImageRes {
        private Long id;
        private String imageUrl;
        private Integer orderIndex;
    }

    /**
     * 판매자 정보 응답
     */
    @Getter @Builder
    public static class SellerInfo {
        private Long userId;
        private String nickname;
    }

    /**
     * 상품 목록 조회 응답 (리스트용)
     */
    @Getter @Builder
    public static class ProductListRes {
        private Long productId;  // id → productId로 변경
        private String title;
        private Integer price;
        private ProductStatus status;
        private String imageUrl;  // thumbnailUrl → imageUrl로 변경 (프론트엔드와 일치)
        private String region;
        private Long likeCount;
        private Integer viewCount;
        private Integer commentCount;  // 댓글 수 추가
        private LocalDateTime createdAt;

        public static ProductListRes of(MarketProduct p, String imageUrl, Long likeCount, Integer commentCount) {
            return ProductListRes.builder()
                    .productId(p.getId())
                    .title(p.getTitle())
                    .price(p.getPrice())
                    .status(p.getStatus())
                    .imageUrl(imageUrl)
                    .region(p.getRegion())
                    .likeCount(likeCount)
                    .viewCount(p.getViewCount())
                    .commentCount(commentCount)
                    .createdAt(p.getCreatedAt())
                    .build();
        }
    }

    /**
     * 상품 상세 조회 응답
     */
    @Getter @Builder
    public static class ProductDetailRes {
        private Long productId;  // id → productId로 변경
        private String title;
        private Integer price;
        private String description;
        private ProductStatus status;
        private ProductCondition productCondition;
        private String region;
        private String brand;
        private String size;
        private String gender;
        private Integer viewCount;
        private Long likeCount;
        private Integer commentCount;  // 댓글 수 추가
        private boolean liked;  // 현재 사용자가 찜했는지 여부
        private boolean isMine;  // 내 상품인지 여부 추가

        // 판매자 정보
        private String sellerNickname;  // seller 객체 대신 직접 nickname 노출
        private Long sellerId;

        // 옷장 아이템 정보
        private Long clothId;
        private String clothName;
        private String clothCategory;

        // 이미지 URL (대표 이미지)
        private String imageUrl;
        // 이미지 목록
        private List<ImageRes> images;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static ProductDetailRes of(
                MarketProduct p,
                Long likeCount,
                Integer commentCount,
                boolean liked,
                boolean isMine,
                List<ImageRes> images,
                Long viewerId
        ) {
            return ProductDetailRes.builder()
                    .productId(p.getId())
                    .title(p.getTitle())
                    .price(p.getPrice())
                    .description(p.getDescription())
                    .status(p.getStatus())
                    .productCondition(p.getProductCondition())
                    .region(p.getRegion())
                    .brand(p.getBrand())
                    .size(p.getSize())
                    .gender(p.getGender())
                    .viewCount(p.getViewCount())
                    .likeCount(likeCount)
                    .commentCount(commentCount)
                    .liked(liked)
                    .isMine(isMine)
                    .sellerNickname(p.getSeller().getNickname())
                    .sellerId(p.getSeller().getUserId())
                    .clothId(p.getCloth().getId())
                    .clothName(p.getCloth().getName())
                    .clothCategory(p.getCloth().getCategory().name())
                    .imageUrl(images.isEmpty() ? null : images.get(0).getImageUrl())  // 대표 이미지 추가
                    .images(images)
                    .createdAt(p.getCreatedAt())
                    .updatedAt(p.getUpdatedAt())
                    .build();
        }
    }
}
