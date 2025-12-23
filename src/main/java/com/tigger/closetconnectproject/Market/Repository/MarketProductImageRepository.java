package com.tigger.closetconnectproject.Market.Repository;

import com.tigger.closetconnectproject.Market.Entity.MarketProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 중고거래 상품 이미지 Repository
 */
public interface MarketProductImageRepository extends JpaRepository<MarketProductImage, Long> {

    /**
     * 상품의 모든 이미지 조회 (순서대로)
     */
    List<MarketProductImage> findByMarketProduct_IdOrderByOrderIndexAsc(Long marketProductId);

    /**
     * 상품의 이미지 삭제
     */
    void deleteByMarketProduct_Id(Long marketProductId);

    /**
     * 여러 상품의 이미지 조회 (N+1 방지)
     */
    @Query("""
        SELECT i
        FROM MarketProductImage i
        WHERE i.marketProduct.id IN :productIds
        ORDER BY i.marketProduct.id, i.orderIndex ASC
        """)
    List<MarketProductImage> findByMarketProduct_IdIn(List<Long> productIds);
}
