package com.tigger.closetconnectproject.Market.Entity;

import com.tigger.closetconnectproject.Closet.Entity.Cloth;
import com.tigger.closetconnectproject.Common.Entity.BaseTimeEntity;
import com.tigger.closetconnectproject.User.Entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

/**
 * 중고거래 상품 엔티티
 */
@Entity
@Table(name = "market_product",
        indexes = {
            @Index(name = "idx_market_status_created", columnList = "status, created_at"),
            @Index(name = "idx_market_seller", columnList = "seller_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MarketProduct extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 판매자
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Users seller;

    // 옷장 아이템 참조
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "cloth_id", nullable = false)
    private Cloth cloth;

    // 상품명
    @Column(nullable = false, length = 200)
    private String title;

    // 가격
    @Column(nullable = false)
    private Integer price;

    // 상품 설명
    @Lob
    @Column(nullable = false)
    private String description;

    // 판매 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.ON_SALE;

    // 상품 상태 (품질)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductCondition productCondition;

    // 거래 지역 (선택 사항)
    @Column(length = 100)
    private String region;

    // 조회수
    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    // 브랜드 (선택 사항)
    @Column(length = 100)
    private String brand;

    // 사이즈 (선택 사항)
    @Column(length = 50)
    private String size;

    // 성별 (선택 사항)
    @Column(length = 20)
    private String gender;

    // 상품 이미지들
    @OneToMany(mappedBy = "marketProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MarketProductImage> images = new ArrayList<>();

    /**
     * 상품 정보 수정
     */
    public void update(String title, Integer price, String description,
                      ProductCondition condition, String region,
                      String brand, String size, String gender) {
        if (title != null) this.title = title;
        if (price != null) this.price = price;
        if (description != null) this.description = description;
        if (condition != null) this.productCondition = condition;
        if (region != null) this.region = region;
        if (brand != null) this.brand = brand;
        if (size != null) this.size = size;
        if (gender != null) this.gender = gender;
    }

    /**
     * 판매 상태 변경
     */
    public void changeStatus(ProductStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 이미지 추가
     */
    public void addImage(MarketProductImage image) {
        this.images.add(image);
        image.setMarketProduct(this);
    }

    /**
     * 판매자 확인
     */
    public boolean isSeller(Long userId) {
        return this.seller.getUserId().equals(userId);
    }
}
