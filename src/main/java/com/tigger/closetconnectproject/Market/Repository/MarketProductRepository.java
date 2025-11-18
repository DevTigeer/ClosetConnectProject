package com.tigger.closetconnectproject.Market.Repository;

import com.tigger.closetconnectproject.Market.Entity.MarketProduct;
import com.tigger.closetconnectproject.Market.Entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 중고거래 상품 Repository
 */
public interface MarketProductRepository extends JpaRepository<MarketProduct, Long> {

    /**
     * 상품 목록 조회 (필터링 + 검색)
     */
    @Query("""
        SELECT p FROM MarketProduct p
        WHERE (:status IS NULL OR p.status = :status)
          AND (:region IS NULL OR p.region LIKE %:region%)
          AND (:keyword IS NULL OR p.title LIKE %:keyword% OR p.description LIKE %:keyword%)
        """)
    Page<MarketProduct> searchProducts(
            @Param("status") ProductStatus status,
            @Param("region") String region,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 판매자별 상품 목록 조회
     */
    Page<MarketProduct> findBySeller_UserId(Long sellerId, Pageable pageable);

    /**
     * 특정 Cloth로 등록된 상품 조회
     */
    List<MarketProduct> findByCloth_Id(Long clothId);

    /**
     * ID로 상품 조회 (판매자 정보 포함)
     */
    @Query("""
        SELECT p FROM MarketProduct p
        LEFT JOIN FETCH p.seller
        LEFT JOIN FETCH p.cloth
        WHERE p.id = :id
        """)
    Optional<MarketProduct> findByIdWithDetails(@Param("id") Long id);
}
