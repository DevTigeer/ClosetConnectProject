package com.tigger.closetconnectproject.Market.Dto;

import com.tigger.closetconnectproject.Market.Entity.Order;
import com.tigger.closetconnectproject.Market.Entity.OrderStatus;
import com.tigger.closetconnectproject.Market.Entity.PaymentMethod;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 주문 관련 DTO
 */
public class OrderDtos {

    /**
     * 주문 생성 요청 DTO
     */
    @Builder
    public record CreateRequest(
            Long productId  // 구매할 상품 ID
    ) {}

    /**
     * 주문 생성 응답 DTO
     * 클라이언트가 토스 결제창을 호출할 때 필요한 정보
     */
    @Builder
    public record CreateResponse(
            Long orderId,           // DB 주문 ID
            String tossOrderId,     // 토스 주문 ID (UUID)
            Integer amount,         // 결제 금액
            String orderName,       // 주문명 (상품명)
            String customerName     // 구매자 이름
    ) {}

    /**
     * 주문 상세 조회 응답 DTO
     */
    @Builder
    public record DetailResponse(
            Long orderId,
            String tossOrderId,

            // 상품 정보
            Long productId,
            String productTitle,
            String productThumbnail,

            // 구매자/판매자 정보
            Long buyerId,
            String buyerNickname,
            Long sellerId,
            String sellerNickname,

            // 결제 정보
            Integer orderAmount,
            OrderStatus orderStatus,
            String orderStatusName,
            PaymentMethod paymentMethod,
            String paymentMethodName,
            String paymentKey,
            LocalDateTime approvedAt,

            // 배송 정보
            String shippingCompany,
            String trackingNumber,
            LocalDateTime shippedAt,

            // 구매 확정/정산
            LocalDateTime confirmedAt,
            LocalDateTime settledAt,

            // 취소/환불
            String cancelReason,
            LocalDateTime refundedAt,

            // 생성/수정 일시
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static DetailResponse from(Order order) {
            return DetailResponse.builder()
                    .orderId(order.getId())
                    .tossOrderId(order.getTossOrderId())
                    .productId(order.getProduct().getId())
                    .productTitle(order.getProduct().getTitle())
                    .productThumbnail(null)  // TODO: 썸네일 조회 필요
                    .buyerId(order.getBuyer().getUserId())
                    .buyerNickname(order.getBuyer().getNickname())
                    .sellerId(order.getSeller().getUserId())
                    .sellerNickname(order.getSeller().getNickname())
                    .orderAmount(order.getOrderAmount())
                    .orderStatus(order.getOrderStatus())
                    .orderStatusName(order.getOrderStatus().getKoreanName())
                    .paymentMethod(order.getPaymentMethod())
                    .paymentMethodName(order.getPaymentMethod() != null ? order.getPaymentMethod().getKoreanName() : null)
                    .paymentKey(order.getPaymentKey())
                    .approvedAt(order.getApprovedAt())
                    .shippingCompany(order.getShippingCompany())
                    .trackingNumber(order.getTrackingNumber())
                    .shippedAt(order.getShippedAt())
                    .confirmedAt(order.getConfirmedAt())
                    .settledAt(order.getSettledAt())
                    .cancelReason(order.getCancelReason())
                    .refundedAt(order.getRefundedAt())
                    .createdAt(order.getCreatedAt())
                    .updatedAt(order.getUpdatedAt())
                    .build();
        }
    }

    /**
     * 주문 목록 조회 응답 DTO (간소화)
     */
    @Builder
    public record ListResponse(
            Long orderId,
            String tossOrderId,
            Long productId,
            String productTitle,
            String productThumbnail,
            Integer orderAmount,
            OrderStatus orderStatus,
            String orderStatusName,
            LocalDateTime createdAt
    ) {
        public static ListResponse from(Order order) {
            return ListResponse.builder()
                    .orderId(order.getId())
                    .tossOrderId(order.getTossOrderId())
                    .productId(order.getProduct().getId())
                    .productTitle(order.getProduct().getTitle())
                    .productThumbnail(null)  // TODO: 썸네일 조회
                    .orderAmount(order.getOrderAmount())
                    .orderStatus(order.getOrderStatus())
                    .orderStatusName(order.getOrderStatus().getKoreanName())
                    .createdAt(order.getCreatedAt())
                    .build();
        }
    }

    /**
     * 발송 처리 요청 DTO
     */
    @Builder
    public record ShipRequest(
            String shippingCompany,  // 배송사 (예: CJ대한통운, 우체국)
            String trackingNumber    // 운송장 번호
    ) {}

    /**
     * 발송 처리 응답 DTO
     */
    @Builder
    public record ShipResponse(
            Long orderId,
            OrderStatus orderStatus,
            String shippingCompany,
            String trackingNumber,
            LocalDateTime shippedAt
    ) {}

    /**
     * 구매 확정 응답 DTO
     */
    @Builder
    public record ConfirmResponse(
            Long orderId,
            OrderStatus orderStatus,
            LocalDateTime confirmedAt
    ) {}

    /**
     * 주문 취소 요청 DTO
     */
    @Builder
    public record CancelRequest(
            String cancelReason
    ) {}

    /**
     * 주문 취소 응답 DTO
     */
    @Builder
    public record CancelResponse(
            Long orderId,
            OrderStatus orderStatus,
            String cancelReason
    ) {}
}
