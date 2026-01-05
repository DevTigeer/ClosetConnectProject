package com.tigger.closetconnectproject.Closet.Util;

import com.tigger.closetconnectproject.Closet.Entity.Category;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AI 세그멘테이션 라벨 (18개)을 앱 카테고리 (4개)로 매핑하는 유틸리티
 */
@Component
public class CategoryMapper {

    private static final Map<String, Category> LABEL_TO_CATEGORY = Map.ofEntries(
            // 상의 관련
            Map.entry("upper-clothes", Category.TOP),
            Map.entry("dress", Category.TOP),

            // 하의 관련
            Map.entry("pants", Category.BOTTOM),
            Map.entry("skirt", Category.BOTTOM),

            // 신발 관련
            Map.entry("left-shoe", Category.SHOES),
            Map.entry("right-shoe", Category.SHOES),

            // 액세서리 관련
            Map.entry("hat", Category.ACC),
            Map.entry("bag", Category.ACC),
            Map.entry("scarf", Category.ACC),
            Map.entry("belt", Category.ACC),
            Map.entry("sunglasses", Category.ACC)
    );

    /**
     * AI 세그멘테이션 라벨을 앱 카테고리로 매핑
     *
     * @param segmentationLabel AI가 감지한 라벨 (e.g., "upper-clothes", "pants")
     * @return 매핑된 앱 카테고리
     */
    public Category mapLabelToCategory(String segmentationLabel) {
        if (segmentationLabel == null) {
            return Category.ACC; // 기본값
        }

        return LABEL_TO_CATEGORY.getOrDefault(
                segmentationLabel.toLowerCase(),
                Category.ACC  // 매핑되지 않은 라벨은 액세서리로 분류
        );
    }

    /**
     * 카테고리 한글 이름 반환
     *
     * @param category 앱 카테고리
     * @return 한글 이름
     */
    public String getCategoryKoreanName(Category category) {
        return switch (category) {
            case TOP -> "상의";
            case BOTTOM -> "하의";
            case SHOES -> "신발";
            case ACC -> "액세서리";
        };
    }
}
