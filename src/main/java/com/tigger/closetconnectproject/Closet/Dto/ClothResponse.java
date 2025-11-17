package com.tigger.closetconnectproject.Closet.Dto;

import com.tigger.closetconnectproject.Closet.Entity.Category;

public record ClothResponse(
        Long id,
        String name,
        Category category,
        String imageUrl,  // 기존 호환성 유지
        String originalImageUrl,  // 원본 이미지 URL
        String removedBgImageUrl  // 배경 제거 이미지 URL
) {}
