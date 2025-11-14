package com.tigger.closetconnectproject.Weather.Dto;

import lombok.Getter;

import java.util.Arrays;

/**
 * Open-Meteo Weather Code → 한국어 변환
 * https://open-meteo.com/en/docs
 */
@Getter
public enum WeatherCode {
    CLEAR(0, "맑음", "☀️"),
    MAINLY_CLEAR(1, "대체로 맑음", "🌤️"),
    PARTLY_CLOUDY(2, "구름 조금", "⛅"),
    OVERCAST(3, "흐림", "☁️"),

    FOG(45, "안개", "🌫️"),
    DEPOSITING_RIME_FOG(48, "착빙 안개", "🌫️"),

    DRIZZLE_LIGHT(51, "약한 이슬비", "🌦️"),
    DRIZZLE_MODERATE(53, "이슬비", "🌦️"),
    DRIZZLE_DENSE(55, "강한 이슬비", "🌧️"),

    FREEZING_DRIZZLE_LIGHT(56, "약한 어는 이슬비", "🌧️"),
    FREEZING_DRIZZLE_DENSE(57, "강한 어는 이슬비", "🌧️"),

    RAIN_SLIGHT(61, "약한 비", "🌧️"),
    RAIN_MODERATE(63, "비", "🌧️"),
    RAIN_HEAVY(65, "강한 비", "🌧️"),

    FREEZING_RAIN_LIGHT(66, "약한 어는 비", "🌧️"),
    FREEZING_RAIN_HEAVY(67, "강한 어는 비", "🌧️"),

    SNOW_SLIGHT(71, "약한 눈", "🌨️"),
    SNOW_MODERATE(73, "눈", "❄️"),
    SNOW_HEAVY(75, "강한 눈", "❄️"),

    SNOW_GRAINS(77, "진눈깨비", "🌨️"),

    RAIN_SHOWERS_SLIGHT(80, "약한 소나기", "🌦️"),
    RAIN_SHOWERS_MODERATE(81, "소나기", "🌧️"),
    RAIN_SHOWERS_VIOLENT(82, "강한 소나기", "⛈️"),

    SNOW_SHOWERS_SLIGHT(85, "약한 눈 소나기", "🌨️"),
    SNOW_SHOWERS_HEAVY(86, "강한 눈 소나기", "❄️"),

    THUNDERSTORM(95, "천둥번개", "⛈️"),
    THUNDERSTORM_SLIGHT_HAIL(96, "약한 우박 동반 천둥번개", "⛈️"),
    THUNDERSTORM_HEAVY_HAIL(99, "강한 우박 동반 천둥번개", "⛈️"),

    UNKNOWN(-1, "알 수 없음", "❓");

    private final int code;
    private final String korean;
    private final String emoji;

    WeatherCode(int code, String korean, String emoji) {
        this.code = code;
        this.korean = korean;
        this.emoji = emoji;
    }

    /**
     * Weather Code 숫자를 Enum으로 변환
     * @param code Open-Meteo weather code
     * @return 해당하는 WeatherCode enum
     */
    public static WeatherCode fromCode(Integer code) {
        if (code == null) return UNKNOWN;
        return Arrays.stream(values())
                .filter(w -> w.code == code)
                .findFirst()
                .orElse(UNKNOWN);
    }
}
