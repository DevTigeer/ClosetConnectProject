package com.tigger.closetconnectproject.Closet.Dto;

import com.tigger.closetconnectproject.Closet.Entity.Category;
import jakarta.validation.constraints.NotNull;

/**
 * 카테고리 확인 요청 DTO
 * - AI 처리 완료 후 사용자가 이미지와 카테고리를 선택하여 제출
 */
public record ConfirmCategoryRequest(
        @NotNull(message = "카테고리는 필수입니다.")
        Category category,

        /**
         * 사용자가 선택한 이미지 타입
         * - ORIGINAL: 원본 이미지
         * - REMOVED_BG: 배경 제거 이미지
         * - SEGMENTED: 세그먼트 이미지 (크롭된 옷)
         * - INPAINTED: 인페인팅 이미지 (AI 복원)
         */
        @NotNull(message = "이미지 선택은 필수입니다.")
        ImageType selectedImageType
) {

    public enum ImageType {
        ORIGINAL,
        REMOVED_BG,
        SEGMENTED,
        INPAINTED
    }
}
