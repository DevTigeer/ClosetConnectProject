package com.tigger.closetconnectproject.Closet.Dto;

import com.tigger.closetconnectproject.Closet.Entity.Category;
import com.tigger.closetconnectproject.Closet.Entity.ProcessingStatus;

/**
 * 옷 처리 상태 조회 응답 DTO
 * - 프론트엔드에서 폴링하여 처리 진행 상황을 확인하기 위한 DTO
 * - WebSocket을 사용하는 경우에도 초기 상태 확인에 사용
 */
public record ClothStatusResponse(
        Long id,
        ProcessingStatus processingStatus,
        String currentStep,  // 현재 진행 중인 단계
        Integer progressPercentage,  // 진행률 (0-100)
        Category suggestedCategory,  // AI가 제안한 카테고리
        String segmentationLabel,  // AI 원본 라벨 (예: "upper-clothes", "pants")
        String segmentedImageUrl,  // 세그멘테이션 결과 이미지
        String inpaintedImageUrl,  // 복원된 최종 이미지
        String errorMessage  // 에러 메시지 (processingStatus가 FAILED일 때)
) {}
