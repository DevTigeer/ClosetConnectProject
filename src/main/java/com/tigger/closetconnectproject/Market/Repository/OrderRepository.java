package com.tigger.closetconnectproject.Market.Repository;

import com.tigger.closetconnectproject.Market.Entity.Order;
import com.tigger.closetconnectproject.Market.Entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 주문 Repository
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 토스 주문 ID로 조회
     */
    Optional<Order> findByTossOrderId(String tossOrderId);

    /**
     * 결제키로 조회
     */
    Optional<Order> findByPaymentKey(String paymentKey);

    /**
     * 구매자별 주문 목록 조회
     */
    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.product p " +
           "JOIN FETCH o.seller " +
           "WHERE o.buyer.userId = :buyerId")
    Page<Order> findByBuyer_UserId(@Param("buyerId") Long buyerId, Pageable pageable);

    /**
     * 판매자별 주문 목록 조회
     */
    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.product p " +
           "JOIN FETCH o.buyer " +
           "WHERE o.seller.userId = :sellerId")
    Page<Order> findBySeller_UserId(@Param("sellerId") Long sellerId, Pageable pageable);

    /**
     * 상품별 주문 목록 조회
     */
    Page<Order> findByProduct_Id(Long productId, Pageable pageable);

    /**
     * 주문 상태별 조회 (구매자)
     */
    Page<Order> findByBuyer_UserIdAndOrderStatus(Long buyerId, OrderStatus status, Pageable pageable);

    /**
     * 주문 상태별 조회 (판매자)
     */
    Page<Order> findBySeller_UserIdAndOrderStatus(Long sellerId, OrderStatus status, Pageable pageable);

    /**
     * 상품에 대한 결제 완료된 주문이 있는지 확인
     */
    boolean existsByProduct_IdAndOrderStatusIn(Long productId, OrderStatus... statuses);

    /**
     * 주문 상세 조회 (페치 조인)
     */
    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.buyer " +
           "JOIN FETCH o.seller " +
           "JOIN FETCH o.product p " +
           "JOIN FETCH p.cloth " +
           "WHERE o.id = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);
}
