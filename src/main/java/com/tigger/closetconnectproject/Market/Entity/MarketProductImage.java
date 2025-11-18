package com.tigger.closetconnectproject.Market.Entity;

import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;

/**
 * 중고거래 상품 이미지 엔티티
 */
@Entity
@Table(name = "market_product_image",
        indexes = @Index(name = "idx_image_product_order", columnList = "market_product_id, order_index"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MarketProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상품 참조
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "market_product_id", nullable = false)
    @Setter
    private MarketProduct marketProduct;

    // 이미지 URL
    @Column(nullable = false, length = 512)
    private String imageUrl;

    // 이미지 순서 (0부터 시작, 0이 대표 이미지)
    @Column(nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;
}
