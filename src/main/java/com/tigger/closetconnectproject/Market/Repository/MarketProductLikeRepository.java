package com.tigger.closetconnectproject.Market.Repository;

import com.tigger.closetconnectproject.Market.Entity.MarketProductLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 중고거래 상품 찜/좋아요 Repository
 */
public interface MarketProductLikeRepository extends JpaRepository<MarketProductLike, Long> {

    /**
     * 사용자가 특정 상품을 찜했는지 확인
     */
    boolean existsByMarketProduct_IdAndUser_UserId(Long marketProductId, Long userId);

    /**
     * 특정 상품의 찜 개수
     */
    long countByMarketProduct_Id(Long marketProductId);

    /**
     * 사용자가 찜한 상품 찜 정보 조회
     */
    Optional<MarketProductLike> findByMarketProduct_IdAndUser_UserId(Long marketProductId, Long userId);

    /**
     * 찜 삭제
     */
    void deleteByMarketProduct_IdAndUser_UserId(Long marketProductId, Long userId);

    /**
     * 사용자가 찜한 상품 목록
     */
    @Query("""
        SELECT l FROM MarketProductLike l
        LEFT JOIN FETCH l.marketProduct p
        WHERE l.user.userId = :userId
        ORDER BY l.createdAt DESC
        """)
    Page<MarketProductLike> findByUser_UserIdWithProduct(Long userId, Pageable pageable);

    /**
     * 여러 상품에 대한 찜 정보 조회 (리스트용)
     */
    List<MarketProductLike> findAllByMarketProduct_IdInAndUser_UserId(List<Long> productIds, Long userId);

    /**
     * 찜 추가 (중복 무시 - MariaDB/MySQL용)
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO market_product_like(market_product_id, user_id, created_at) " +
                   "VALUES (?1, ?2, NOW())", nativeQuery = true)
    void insert(Long marketProductId, Long userId);

    /**
     * 여러 상품의 찜 개수 조회 (N+1 방지)
     */
    @Query("""
        SELECT l.marketProduct.id, COUNT(l)
        FROM MarketProductLike l
        WHERE l.marketProduct.id IN :productIds
        GROUP BY l.marketProduct.id
        """)
    List<Object[]> countByMarketProduct_IdIn(List<Long> productIds);
}
