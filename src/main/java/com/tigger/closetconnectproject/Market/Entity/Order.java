package com.tigger.closetconnectproject.Market.Entity;

import com.tigger.closetconnectproject.Common.Entity.BaseTimeEntity;
import com.tigger.closetconnectproject.User.Entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

/**
 * 주문 엔티티
 * 중고거래 결제 및 에스크로 흐름 관리
 */
@Entity
@Table(name = "orders",
        indexes = {
            @Index(name = "idx_order_buyer", columnList = "buyer_id"),
            @Index(name = "idx_order_seller", columnList = "seller_id"),
            @Index(name = "idx_order_product", columnList = "product_id"),
            @Index(name = "idx_order_toss_order_id", columnList = "toss_order_id"),
            @Index(name = "idx_order_payment_key", columnList = "payment_key")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 토스페이먼츠 주문 ID (UUID 형식, 중복 불가)
    @Column(name = "toss_order_id", nullable = false, unique = true, length = 100)
    private String tossOrderId;

    // 구매자
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Users buyer;

    // 판매자
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Users seller;

    // 상품
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private MarketProduct product;

    // 주문 금액 (실제 결제 금액)
    @Column(nullable = false)
    private Integer orderAmount;

    // 주문 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PAYMENT_PENDING;

    // ========== 결제 관련 정보 ==========

    // 토스페이먼츠 결제키 (결제 승인 후 발급)
    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    // 결제 수단
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod paymentMethod;

    // 결제 승인 일시
    @Column
    private LocalDateTime approvedAt;

    // 취소/환불 사유
    @Column(length = 500)
    private String cancelReason;

    // 환불 일시
    @Column
    private LocalDateTime refundedAt;

    // ========== 배송 관련 정보 ==========

    // 배송사
    @Column(length = 50)
    private String shippingCompany;

    // 운송장 번호
    @Column(length = 100)
    private String trackingNumber;

    // 발송 일시
    @Column
    private LocalDateTime shippedAt;

    // ========== 구매 확정 및 정산 ==========

    // 구매 확정 일시
    @Column
    private LocalDateTime confirmedAt;

    // 정산 완료 일시
    @Column
    private LocalDateTime settledAt;

    // ========== 비즈니스 로직 메서드 ==========

    /**
     * 결제 승인 처리
     */
    public void approvePayment(String paymentKey, PaymentMethod method) {
        this.paymentKey = paymentKey;
        this.paymentMethod = method;
        this.orderStatus = OrderStatus.PAYMENT_PAID;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * 발송 처리 (판매자)
     */
    public void ship(String shippingCompany, String trackingNumber) {
        if (!this.orderStatus.isShippable()) {
            throw new IllegalStateException("발송 가능한 상태가 아닙니다. 현재 상태: " + this.orderStatus.getKoreanName());
        }
        this.shippingCompany = shippingCompany;
        this.trackingNumber = trackingNumber;
        this.orderStatus = OrderStatus.SHIPPED;
        this.shippedAt = LocalDateTime.now();
    }

    /**
     * 구매 확정 처리 (구매자)
     */
    public void confirm() {
        if (!this.orderStatus.isConfirmable()) {
            throw new IllegalStateException("구매 확정 가능한 상태가 아닙니다. 현재 상태: " + this.orderStatus.getKoreanName());
        }
        this.orderStatus = OrderStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    /**
     * 정산 완료 처리 (시스템)
     */
    public void settle() {
        if (this.orderStatus != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("정산은 구매 확정 후에만 가능합니다.");
        }
        this.orderStatus = OrderStatus.SETTLEMENT_RELEASED;
        this.settledAt = LocalDateTime.now();
    }

    /**
     * 주문 취소
     */
    public void cancel(String reason) {
        if (!this.orderStatus.isCancellable()) {
            throw new IllegalStateException("취소 불가능한 상태입니다. 현재 상태: " + this.orderStatus.getKoreanName());
        }
        this.orderStatus = OrderStatus.CANCELLED;
        this.cancelReason = reason;
    }

    /**
     * 환불 처리
     */
    public void refund(String reason) {
        this.orderStatus = OrderStatus.REFUNDED;
        this.cancelReason = reason;
        this.refundedAt = LocalDateTime.now();
    }

    /**
     * 구매자 확인
     */
    public boolean isBuyer(Long userId) {
        return this.buyer.getUserId().equals(userId);
    }

    /**
     * 판매자 확인
     */
    public boolean isSeller(Long userId) {
        return this.seller.getUserId().equals(userId);
    }

    /**
     * 주문 참여자 확인 (구매자 또는 판매자)
     */
    public boolean isParticipant(Long userId) {
        return isBuyer(userId) || isSeller(userId);
    }
}
