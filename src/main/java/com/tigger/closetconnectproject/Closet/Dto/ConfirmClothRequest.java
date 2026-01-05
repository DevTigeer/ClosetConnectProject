package com.tigger.closetconnectproject.Closet.Dto;

import com.tigger.closetconnectproject.Closet.Entity.Category;

public record ConfirmClothRequest(
        ImageType selectedImageType,  // 표준 이미지 타입 (ORIGINAL, REMOVED_BG, SEGMENTED, INPAINTED)

        String selectedImageUrl,  // 직접 선택한 이미지 URL (추가 아이템 선택 시)

        Category category  // 선택적: AI 제안 카테고리를 수정할 수 있음
) {
    public enum ImageType {
        ORIGINAL,      // 원본 이미지
        REMOVED_BG,    // 배경 제거 이미지
        SEGMENTED,     // 세그멘테이션 이미지 (크롭된 옷)
        INPAINTED      // 인페인팅 이미지 (복원된 최종)
    }

    /**
     * 최소 하나의 이미지 선택 방법이 제공되었는지 검증
     */
    public boolean hasValidSelection() {
        return selectedImageType != null || (selectedImageUrl != null && !selectedImageUrl.isBlank());
    }
}
