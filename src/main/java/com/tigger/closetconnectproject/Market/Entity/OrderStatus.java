package com.tigger.closetconnectproject.Market.Entity;

/**
 * 주문 상태
 * 중고거래 에스크로 흐름에 맞춘 상태 모델
 */
public enum OrderStatus {
    /**
     * 결제 대기 (주문 생성됨, 결제 미완료)
     */
    PAYMENT_PENDING,

    /**
     * 결제 완료 (토스 결제 승인 완료, 판매자 발송 대기)
     */
    PAYMENT_PAID,

    /**
     * 발송 완료 (판매자가 운송장 번호 입력 완료)
     */
    SHIPPED,

    /**
     * 구매 확정 (구매자가 상품 수령 및 구매 확정)
     */
    CONFIRMED,

    /**
     * 정산 완료 (판매자에게 정산 처리 완료)
     * 1차 버전에서는 CONFIRMED 후 자동으로 전환 가능
     */
    SETTLEMENT_RELEASED,

    /**
     * 취소 (결제 전 또는 결제 후 취소)
     */
    CANCELLED,

    /**
     * 환불 (결제 완료 후 환불 처리)
     */
    REFUNDED;

    /**
     * 취소 가능 여부
     */
    public boolean isCancellable() {
        return this == PAYMENT_PENDING || this == PAYMENT_PAID;
    }

    /**
     * 발송 가능 여부 (판매자)
     */
    public boolean isShippable() {
        return this == PAYMENT_PAID;
    }

    /**
     * 구매 확정 가능 여부 (구매자)
     */
    public boolean isConfirmable() {
        return this == SHIPPED;
    }

    /**
     * 한글 상태명
     */
    public String getKoreanName() {
        return switch (this) {
            case PAYMENT_PENDING -> "결제대기";
            case PAYMENT_PAID -> "결제완료";
            case SHIPPED -> "발송완료";
            case CONFIRMED -> "구매확정";
            case SETTLEMENT_RELEASED -> "정산완료";
            case CANCELLED -> "취소";
            case REFUNDED -> "환불";
        };
    }
}
