package com.tigger.closetconnectproject.Market.Dto;

import com.tigger.closetconnectproject.Market.Entity.PaymentMethod;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 결제 관련 DTO
 */
public class PaymentDtos {

    /**
     * 결제 승인 요청 DTO
     */
    @Builder
    public record ConfirmRequest(
            String paymentKey,     // 토스에서 발급한 결제 키
            String orderId,        // 주문 ID (tossOrderId)
            Integer amount         // 결제 금액 (검증용)
    ) {}

    /**
     * 토스페이먼츠 결제 승인 API 응답 DTO
     */
    @Builder
    public record TossPaymentResponse(
            String mId,            // 상점 아이디 (MID)
            String paymentKey,
            String orderId,
            String status,
            String method,
            Integer totalAmount,
            Integer balanceAmount,
            String approvedAt,
            CardInfo card,
            VirtualAccountInfo virtualAccount,
            TransferInfo transfer,
            MobilePhoneInfo mobilePhone,
            GiftCertificateInfo giftCertificate,
            EasyPayInfo easyPay
    ) {
        @Builder
        public record CardInfo(
                String issuerCode,
                String acquirerCode,
                String number,
                Integer installmentPlanMonths,
                String approveNo
        ) {}

        @Builder
        public record VirtualAccountInfo(
                String accountNumber,
                String bankCode,
                String customerName,
                String dueDate
        ) {}

        @Builder
        public record TransferInfo(
                String bankCode,
                String settlementStatus
        ) {}

        @Builder
        public record MobilePhoneInfo(
                String customerMobilePhone,
                String settlementStatus
        ) {}

        @Builder
        public record GiftCertificateInfo(
                String approveNo,
                String settlementStatus
        ) {}

        @Builder
        public record EasyPayInfo(
                String provider,
                Integer amount,
                Integer discountAmount
        ) {}

        /**
         * 결제 수단을 enum으로 변환
         */
        public PaymentMethod getPaymentMethodEnum() {
            if (method == null) return null;
            return switch (method.toUpperCase()) {
                case "CARD", "카드" -> PaymentMethod.CARD;
                case "VIRTUAL_ACCOUNT", "가상계좌" -> PaymentMethod.VIRTUAL_ACCOUNT;
                case "TRANSFER", "계좌이체" -> PaymentMethod.TRANSFER;
                case "MOBILE_PHONE", "휴대폰" -> PaymentMethod.MOBILE_PHONE;
                case "GIFT_CERTIFICATE", "상품권" -> PaymentMethod.GIFT_CERTIFICATE;
                case "EASY_PAY", "간편결제" -> PaymentMethod.EASY_PAY;
                default -> null;
            };
        }
    }

    /**
     * 결제 승인 성공 응답 DTO
     */
    @Builder
    public record ConfirmResponse(
            Long orderId,
            String tossOrderId,
            String paymentKey,
            String status,
            Integer amount,
            String paymentMethod,
            LocalDateTime approvedAt
    ) {}

    /**
     * 결제 취소 요청 DTO
     */
    @Builder
    public record CancelRequest(
            String cancelReason
    ) {}

    /**
     * 결제 취소 응답 DTO
     */
    @Builder
    public record CancelResponse(
            String paymentKey,
            String orderId,
            String status,
            String cancelReason,
            LocalDateTime canceledAt
    ) {}
}
