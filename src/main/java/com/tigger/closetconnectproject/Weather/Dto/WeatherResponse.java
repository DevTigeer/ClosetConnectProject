package com.tigger.closetconnectproject.Weather.Dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 날씨 API 응답 DTO
 */
@Getter
@Builder
public class WeatherResponse {

    // 위치 정보
    private String cityName;      // 도시명 (있으면)
    private Double latitude;      // 위도
    private Double longitude;     // 경도

    // 현재 날씨
    private CurrentWeather current;

    // 오늘 날씨
    private DailyWeather today;

    // 시간별 날씨 (24시간)
    private List<HourlyWeather> hourly;

    /**
     * 현재 날씨 정보
     */
    @Getter
    @Builder
    public static class CurrentWeather {
        private Double temperature;        // 현재 기온 (°C)
        private Integer weatherCode;       // 날씨 코드
        private String weatherDescription; // 날씨 설명 (한글)
        private String weatherEmoji;       // 날씨 이모지
        private Double windSpeed;          // 풍속 (km/h)
        private Integer windDirection;     // 풍향 (도)
        private String time;               // 측정 시각
    }

    /**
     * 일별 날씨 정보
     */
    @Getter
    @Builder
    public static class DailyWeather {
        private String date;               // 날짜 (YYYY-MM-DD)
        private Double temperatureMax;     // 최고 기온 (°C)
        private Double temperatureMin;     // 최저 기온 (°C)
        private Integer weatherCode;       // 날씨 코드
        private String weatherDescription; // 날씨 설명 (한글)
    }

    /**
     * 시간별 날씨 정보
     */
    @Getter
    @Builder
    public static class HourlyWeather {
        private String time;               // 시각 (ISO8601)
        private Double temperature;        // 기온 (°C)
        private Double precipitation;      // 강수량 (mm)
        private Integer weatherCode;       // 날씨 코드 (선택)
    }
}
