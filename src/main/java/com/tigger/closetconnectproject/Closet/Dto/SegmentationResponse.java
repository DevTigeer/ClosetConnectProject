package com.tigger.closetconnectproject.Closet.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Cloth Segmentation API 응답 DTO
 * - Python cloth_segmentation_api.py의 응답 형식
 */
public record SegmentationResponse(
        String status,  // "success" or "no_clothes_detected"
        String message,
        @JsonProperty("detected_items")
        List<DetectedItem> detectedItems,
        Summary summary
) {
    public record DetectedItem(
            String label,  // e.g., "upper-clothes", "pants", "skirt"
            @JsonProperty("category_kr")
            String categoryKr,  // e.g., "상의", "하의"
            BoundingBox bbox,
            @JsonProperty("saved_path")
            String savedPath,  // 저장된 크롭 이미지 경로
            @JsonProperty("area_pixels")
            int areaPixels  // 옷 영역 픽셀 수 (크기 비교용)
    ) {}

    public record BoundingBox(
            @JsonProperty("x_min")
            int xMin,
            @JsonProperty("y_min")
            int yMin,
            @JsonProperty("x_max")
            int xMax,
            @JsonProperty("y_max")
            int yMax
    ) {}

    public record Summary(
            @JsonProperty("total_items")
            int totalItems,
            List<String> categories
    ) {}
}
