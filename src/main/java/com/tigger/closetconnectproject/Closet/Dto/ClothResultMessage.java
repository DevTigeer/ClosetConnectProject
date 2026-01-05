package com.tigger.closetconnectproject.Closet.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * RabbitMQ 옷 처리 결과 메시지 (Python → Spring)
 * - Python worker가 처리 완료 후 Spring으로 전송
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClothResultMessage implements Serializable {

    /**
     * 옷 ID
     */
    private Long clothId;

    /**
     * 처리 성공 여부
     */
    private Boolean success;

    /**
     * 에러 메시지 (실패 시)
     */
    private String errorMessage;

    /**
     * 배경 제거 이미지 파일 경로 (Python 서버가 저장한 경로)
     */
    private String removedBgImagePath;

    /**
     * 세그먼트된 이미지 파일 경로 (크롭된 이미지)
     */
    private String segmentedImagePath;

    /**
     * 인페인팅된 이미지 파일 경로
     */
    private String inpaintedImagePath;

    /**
     * 제안된 카테고리 (예: "TOP", "BOTTOM", "OUTER" 등)
     */
    private String suggestedCategory;

    /**
     * AI 세그멘테이션 라벨 (예: "short_sleeve_top", "trousers" 등)
     */
    private String segmentationLabel;

    /**
     * 세그먼트 영역 크기 (pixels)
     */
    private Integer areaPixels;

    /**
     * 추가로 감지된 옷 아이템들 (상의, 하의, 신발, 악세서리 등)
     * - 주 아이템 외에 감지된 모든 아이템 목록
     * @deprecated 대신 allSegmentedItems, allExpandedItems 사용
     */
    @Deprecated
    private List<AdditionalClothingItem> additionalClothingItems;

    /**
     * 모든 세그먼트된 아이템들 (크기순 정렬)
     * - 모든 크롭된 이미지 경로 포함
     */
    private List<SegmentedItem> allSegmentedItems;

    /**
     * 모든 Gemini 확장된 아이템들 (크기순 정렬)
     * - 모든 Gemini로 확장된 이미지 경로 포함
     */
    private List<ExpandedItem> allExpandedItems;

    /**
     * 추가 감지된 옷 아이템 정보 (deprecated)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdditionalClothingItem implements Serializable {
        /**
         * 옷 라벨 (예: "upper-clothes", "pants", "shoes" 등)
         */
        private String label;

        /**
         * 이미지 파일 경로 (Python 서버가 저장한 경로)
         */
        private String path;

        /**
         * 세그먼트 영역 크기 (pixels)
         */
        private Integer areaPixels;
    }

    /**
     * 세그먼트된 아이템 정보 (크롭된 이미지)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentedItem implements Serializable {
        private String label;
        private String segmentedPath;
        private Integer areaPixels;
    }

    /**
     * Gemini 확장된 아이템 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpandedItem implements Serializable {
        private String label;
        private String expandedPath;
        private Integer areaPixels;
    }
}
