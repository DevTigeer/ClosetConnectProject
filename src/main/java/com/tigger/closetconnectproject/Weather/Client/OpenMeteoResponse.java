package com.tigger.closetconnectproject.Weather.Client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Open-Meteo API 응답 매핑 클래스
 * https://open-meteo.com/en/docs
 */
@Data
public class OpenMeteoResponse {

    private Double latitude;
    private Double longitude;
    private String timezone;

    @JsonProperty("current_weather")
    private CurrentWeather currentWeather;

    private HourlyData hourly;
    private DailyData daily;

    /**
     * 현재 날씨
     */
    @Data
    public static class CurrentWeather {
        private String time;
        private Double temperature;

        @JsonProperty("weathercode")
        private Integer weatherCode;

        @JsonProperty("windspeed")
        private Double windSpeed;

        @JsonProperty("winddirection")
        private Integer windDirection;
    }

    /**
     * 시간별 데이터
     */
    @Data
    public static class HourlyData {
        private List<String> time;

        @JsonProperty("temperature_2m")
        private List<Double> temperature2m;

        private List<Double> precipitation;
    }

    /**
     * 일별 데이터
     */
    @Data
    public static class DailyData {
        private List<String> time;

        @JsonProperty("temperature_2m_max")
        private List<Double> temperature2mMax;

        @JsonProperty("temperature_2m_min")
        private List<Double> temperature2mMin;

        @JsonProperty("weathercode")
        private List<Integer> weatherCode;
    }
}
