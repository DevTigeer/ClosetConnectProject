package com.tigger.closetconnectproject.Closet.Dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Outfit Try-On 관련 DTO 모음
 */
public class OutfitDtos {

    /**
     * Outfit Try-On 생성 요청 DTO
     */
    public record CreateTryonRequest(
            Long upperClothesId,      // 상의 ID (선택)
            Long lowerClothesId,      // 하의 ID (선택)
            Long shoesId,             // 신발 ID (선택)
            List<Long> accessoriesIds, // 악세서리 ID 리스트 (선택)
            String prompt             // 커스텀 프롬프트 (선택)
    ) {
        public CreateTryonRequest {
            // 최소 1개 이상의 의류 아이템 필요
            if (upperClothesId == null && lowerClothesId == null &&
                shoesId == null && (accessoriesIds == null || accessoriesIds.isEmpty())) {
                throw new IllegalArgumentException("최소 1개 이상의 의류 아이템이 필요합니다.");
            }
        }
    }

    /**
     * Outfit Try-On 응답 DTO
     */
    public record TryonResponse(
            boolean success,
            String imageUrl,          // 생성된 이미지 URL
            String engine,            // 사용된 엔진 (Gemini, ComfyUI 등)
            String message
    ) {
        public static TryonResponse success(String imageUrl, String engine) {
            return new TryonResponse(true, imageUrl, engine, "Try-on 생성 성공");
        }

        public static TryonResponse failure(String message) {
            return new TryonResponse(false, null, null, message);
        }
    }

    /**
     * Outfit 조합 정보 (저장용, 선택적)
     */
    public record OutfitCombinationDto(
            Long id,
            Long userId,
            Long upperClothesId,
            Long lowerClothesId,
            Long shoesId,
            List<Long> accessoriesIds,
            String resultImageUrl,
            String createdAt
    ) {}
}
