package com.tigger.closetconnectproject.Market.Entity;

/**
 * 결제 수단
 * 토스페이먼츠 지원 결제 수단
 */
public enum PaymentMethod {
    /**
     * 카드 결제
     */
    CARD,

    /**
     * 가상계좌
     */
    VIRTUAL_ACCOUNT,

    /**
     * 계좌이체
     */
    TRANSFER,

    /**
     * 휴대폰 결제
     */
    MOBILE_PHONE,

    /**
     * 상품권
     */
    GIFT_CERTIFICATE,

    /**
     * 간편결제 (토스페이, 네이버페이 등)
     */
    EASY_PAY;

    /**
     * 한글 이름
     */
    public String getKoreanName() {
        return switch (this) {
            case CARD -> "카드";
            case VIRTUAL_ACCOUNT -> "가상계좌";
            case TRANSFER -> "계좌이체";
            case MOBILE_PHONE -> "휴대폰";
            case GIFT_CERTIFICATE -> "상품권";
            case EASY_PAY -> "간편결제";
        };
    }
}
