package com.tigger.closetconnectproject.Weather.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 한국 주요 도시 프리셋 정보
 */
@Getter
@AllArgsConstructor
public class CityPreset {
    private String code;      // 도시 코드 (seoul, busan 등)
    private String name;      // 도시명 (한글)
    private Double latitude;  // 위도
    private Double longitude; // 경도

    /**
     * 한국 주요 도시 프리셋 목록
     */
    public static CityPreset[] KOREAN_CITIES = {
        new CityPreset("seoul", "서울", 37.57, 126.98),
        new CityPreset("incheon", "인천", 37.45, 126.70),
        new CityPreset("busan", "부산", 35.18, 129.07),
        new CityPreset("daegu", "대구", 35.87, 128.60),
        new CityPreset("gwangju", "광주", 35.16, 126.85),
        new CityPreset("jeju", "제주", 33.50, 126.52)
    };
}
