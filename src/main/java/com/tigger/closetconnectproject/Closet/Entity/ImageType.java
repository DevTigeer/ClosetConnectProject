package com.tigger.closetconnectproject.Closet.Entity;

/**
 * 이미지 타입 (전신 사진 vs 단일 옷 이미지)
 * - FULL_BODY: 사람이 옷을 입고 있는 전신 사진 (Segformer 모델 사용)
 * - SINGLE_ITEM: 옷만 촬영한 단일 이미지 (U2NET 모델 사용)
 */
public enum ImageType {
    FULL_BODY,      // 전신 사진 (사람 이미지)
    SINGLE_ITEM     // 단일 옷 이미지
}
