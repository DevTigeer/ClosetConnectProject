package com.tigger.closetconnectproject.Weather.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 온도 구간별 옷차림 추천 기준
 * - 한국 기상청 및 일반적인 체감 기준 참고
 */
@Getter
@AllArgsConstructor
public enum TemperatureRange {

    VERY_HOT(28, 100,
            "민소매, 반팔티, 반팔 셔츠",
            "반바지, 짧은 치마, 린넨 바지",
            "선글라스, 모자, 얇은 가디건(실내 냉방 대비)"),

    HOT(23, 27,
            "반팔티, 얇은 셔츠, 블라우스",
            "반바지, 면바지, 치마",
            "얇은 가디건, 모자, 선글라스"),

    WARM(20, 22,
            "긴팔티, 얇은 니트, 맨투맨",
            "면바지, 청바지, 슬랙스",
            "얇은 가디건, 후드집업"),

    MILD(17, 19,
            "얇은 니트, 맨투맨, 후드티, 긴팔티",
            "청바지, 면바지, 슬랙스",
            "얇은 재킷, 가디건, 바람막이"),

    COOL(12, 16,
            "니트, 맨투맨, 후드티, 긴팔 셔츠",
            "청바지, 면바지, 슬랙스",
            "자켓, 가디건, 야상, 청자켓"),

    CHILLY(9, 11,
            "니트, 스웨터, 긴팔 셔츠",
            "청바지, 기모 바지, 슬랙스",
            "트렌치코트, 야상, 가죽자켓, 스타킹"),

    COLD(5, 8,
            "니트, 스웨터, 기모 후드티",
            "청바지, 기모 바지, 두꺼운 슬랙스",
            "코트, 가죽자켓, 히트텍, 머플러, 장갑"),

    VERY_COLD(-100, 4,
            "두꺼운 니트, 기모 후드티, 내복",
            "기모 바지, 두꺼운 청바지",
            "패딩, 두꺼운 코트, 목도리, 장갑, 방한용품");

    private final int minTemp;  // 최소 온도 (포함)
    private final int maxTemp;  // 최대 온도 (포함)
    private final String topRecommendation;     // 추천 상의
    private final String bottomRecommendation;  // 추천 하의
    private final String extraRecommendation;   // 추가 아이템

    /**
     * 체감 온도에 맞는 온도 구간 찾기
     * @param feelsLikeTemp 체감 온도
     * @return 해당하는 TemperatureRange
     */
    public static TemperatureRange fromTemperature(double feelsLikeTemp) {
        return Arrays.stream(values())
                .filter(range -> feelsLikeTemp >= range.minTemp && feelsLikeTemp <= range.maxTemp)
                .findFirst()
                .orElse(MILD); // 기본값: 17-19도 구간
    }
}
