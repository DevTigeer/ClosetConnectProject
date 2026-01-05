package com.tigger.closetconnectproject.Closet.Dto;

import com.tigger.closetconnectproject.Closet.Entity.Category;
import com.tigger.closetconnectproject.Closet.Entity.ProcessingStatus;

import java.util.List;

public record ClothResponse(
        Long id,
        Long userId,  // 사용자 ID (WebSocket 구독 경로용)
        String name,
        Category category,
        String imageUrl,  // 기존 호환성 유지 (최종 이미지)
        String originalImageUrl,  // 원본 이미지 URL
        String removedBgImageUrl,  // 배경 제거 이미지 URL
        String segmentedImageUrl,  // 세그멘테이션 결과 이미지 URL (크롭된 옷)
        String inpaintedImageUrl,  // Inpainting 결과 이미지 URL (복원된 최종 이미지)
        ProcessingStatus processingStatus,  // 처리 상태
        Category suggestedCategory,  // AI가 제안한 카테고리
        String segmentationLabel,  // AI 원본 라벨
        String errorMessage,  // 에러 메시지 (FAILED 상태일 때)
        List<AdditionalItemResponse> additionalItems,  // 추가 감지된 아이템들 (deprecated)
        List<SegmentedItemResponse> allSegmentedItems,  // 모든 크롭된 아이템들 (크기순)
        List<ExpandedItemResponse> allExpandedItems  // 모든 Gemini 확장된 아이템들 (크기순)
) {
    /**
     * 추가 감지된 옷 아이템 응답 DTO (deprecated)
     */
    public record AdditionalItemResponse(
            String label,      // 아이템 라벨 (예: "upper-clothes", "pants")
            String imageUrl,   // 이미지 URL
            Integer areaPixels // 면적 (pixels)
    ) {}

    /**
     * 세그먼트된 아이템 응답 DTO (크롭된 이미지)
     */
    public record SegmentedItemResponse(
            String label,           // 아이템 라벨
            String segmentedUrl,    // 크롭된 이미지 URL
            Integer areaPixels      // 면적 (pixels)
    ) {}

    /**
     * Gemini 확장된 아이템 응답 DTO
     */
    public record ExpandedItemResponse(
            String label,          // 아이템 라벨
            String expandedUrl,    // Gemini 확장 이미지 URL
            Integer areaPixels     // 면적 (pixels)
    ) {}
}
