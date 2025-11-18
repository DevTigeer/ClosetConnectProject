package com.tigger.closetconnectproject.Market.Entity;

import com.tigger.closetconnectproject.User.Entity.Users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

/**
 * 중고거래 상품 찜/좋아요 엔티티
 */
@Entity
@Table(name = "market_product_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_like_product_user",
                columnNames = {"market_product_id", "user_id"}
        ),
        indexes = {
            @Index(name = "idx_like_product", columnList = "market_product_id"),
            @Index(name = "idx_like_user", columnList = "user_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MarketProductLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상품 참조
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "market_product_id", nullable = false)
    private MarketProduct marketProduct;

    // 사용자
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    // 찜한 시간
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
