package com.tigger.closetconnectproject.Weather.Dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 날씨 기반 옷 추천 응답 DTO
 */
@Getter
@Builder
public class ClothingRecommendation {

    // 기본 정보
    private Double currentTemperature;    // 현재 온도
    private Double feelsLikeTemperature;  // 체감 온도
    private String weatherCondition;      // 날씨 상태 (맑음, 흐림, 비 등)
    private Integer humidity;             // 습도

    // 추천 아이템
    private String topRecommendation;     // 추천 상의
    private String bottomRecommendation;  // 추천 하의
    private String extraRecommendation;   // 추가 아이템

    // 최종 추천 문장
    private String recommendationMessage;
}
