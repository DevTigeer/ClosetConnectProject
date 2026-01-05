package com.tigger.closetconnectproject.Weather.Service;

import com.tigger.closetconnectproject.Weather.Client.OpenMeteoResponse;
import com.tigger.closetconnectproject.Weather.Dto.CityPreset;
import com.tigger.closetconnectproject.Weather.Dto.WeatherCode;
import com.tigger.closetconnectproject.Weather.Dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Open-Meteo API를 사용한 날씨 정보 조회 Service
 */
@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final String API_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String TIMEZONE = "Asia/Seoul";

    // 간단한 메모리 캐시 (10분 TTL)
    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 10 * 60 * 1000; // 10분

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 설명: 한국 주요 도시 프리셋 목록 반환
     * @return 도시 프리셋 배열
     */
    public CityPreset[] getCityPresets() {
        return CityPreset.KOREAN_CITIES;
    }

    /**
     * 설명: 특정 위치의 현재 날씨 + 오늘/시간별 예보 조회
     * - 10분 캐싱 적용 (같은 좌표 재요청 시 캐시 사용)
     * @param latitude 위도
     * @param longitude 경도
     * @return 날씨 정보 (현재 + 오늘 + 시간별 24시간)
     */
    public WeatherResponse getCurrentWeather(Double latitude, Double longitude) {
        // 소수점 2자리로 반올림 (캐시 키 통일)
        String cacheKey = String.format("%.2f,%.2f", latitude, longitude);

        // 캐시 확인
        CachedWeather cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        // API 호출
        String url = buildApiUrl(latitude, longitude);
        OpenMeteoResponse response = restTemplate.getForObject(url, OpenMeteoResponse.class);

        if (response == null) {
            throw new IllegalArgumentException("날씨 정보를 가져올 수 없습니다.");
        }

        // 응답 변환
        WeatherResponse weatherData = convertToWeatherResponse(response);

        // 캐시 저장
        cache.put(cacheKey, new CachedWeather(weatherData));

        return weatherData;
    }

    /**
     * 설명: 도시 코드로 날씨 조회
     * @param cityCode 도시 코드 (seoul, busan 등)
     * @return 날씨 정보
     */
    public WeatherResponse getWeatherByCity(String cityCode) {
        CityPreset city = Arrays.stream(CityPreset.KOREAN_CITIES)
                .filter(c -> c.getCode().equalsIgnoreCase(cityCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 도시 코드: " + cityCode));

        WeatherResponse weather = getCurrentWeather(city.getLatitude(), city.getLongitude());

        // 도시명 추가
        return WeatherResponse.builder()
                .cityName(city.getName())
                .latitude(weather.getLatitude())
                .longitude(weather.getLongitude())
                .current(weather.getCurrent())
                .today(weather.getToday())
                .hourly(weather.getHourly())
                .build();
    }

    /**
     * API URL 생성
     */
    private String buildApiUrl(Double latitude, Double longitude) {
        return String.format(
            "%s?latitude=%.4f&longitude=%.4f" +
            "&current_weather=true" +
            "&hourly=temperature_2m,precipitation" +
            "&daily=temperature_2m_max,temperature_2m_min,weathercode" +
            "&timezone=%s" +
            "&forecast_days=1",
            API_URL, latitude, longitude, TIMEZONE
        );
    }

    /**
     * Open-Meteo 응답 → WeatherResponse 변환
     */
    private WeatherResponse convertToWeatherResponse(OpenMeteoResponse response) {
        // 현재 날씨
        WeatherResponse.CurrentWeather current = buildCurrentWeather(response.getCurrentWeather());

        // 오늘 날씨 (daily 첫 번째 항목)
        WeatherResponse.DailyWeather today = buildTodayWeather(response.getDaily());

        // 시간별 날씨 (24시간)
        List<WeatherResponse.HourlyWeather> hourly = buildHourlyWeather(response.getHourly());

        return WeatherResponse.builder()
                .latitude(response.getLatitude())
                .longitude(response.getLongitude())
                .current(current)
                .today(today)
                .hourly(hourly)
                .build();
    }

    /**
     * 현재 날씨 정보 생성
     */
    private WeatherResponse.CurrentWeather buildCurrentWeather(OpenMeteoResponse.CurrentWeather cw) {
        if (cw == null) return null;

        WeatherCode weatherCode = WeatherCode.fromCode(cw.getWeatherCode());

        return WeatherResponse.CurrentWeather.builder()
                .temperature(cw.getTemperature())
                .weatherCode(cw.getWeatherCode())
                .weatherDescription(weatherCode.getKorean())
                .weatherEmoji(weatherCode.getEmoji())
                .windSpeed(cw.getWindSpeed())
                .windDirection(cw.getWindDirection())
                .time(cw.getTime())
                .build();
    }

    /**
     * 오늘 날씨 정보 생성
     */
    private WeatherResponse.DailyWeather buildTodayWeather(OpenMeteoResponse.DailyData daily) {
        if (daily == null || daily.getTime() == null || daily.getTime().isEmpty()) {
            return null;
        }

        // 첫 번째 날짜 (오늘)
        String date = daily.getTime().get(0);
        Double tempMax = daily.getTemperature2mMax().get(0);
        Double tempMin = daily.getTemperature2mMin().get(0);
        Integer weatherCodeValue = daily.getWeatherCode().get(0);

        WeatherCode weatherCode = WeatherCode.fromCode(weatherCodeValue);

        return WeatherResponse.DailyWeather.builder()
                .date(date)
                .temperatureMax(tempMax)
                .temperatureMin(tempMin)
                .weatherCode(weatherCodeValue)
                .weatherDescription(weatherCode.getKorean())
                .build();
    }

    /**
     * 시간별 날씨 정보 생성 (24시간)
     */
    private List<WeatherResponse.HourlyWeather> buildHourlyWeather(OpenMeteoResponse.HourlyData hourly) {
        if (hourly == null || hourly.getTime() == null || hourly.getTime().isEmpty()) {
            return Collections.emptyList();
        }

        List<WeatherResponse.HourlyWeather> result = new ArrayList<>();

        // 최대 24시간 데이터만 반환
        int limit = Math.min(24, hourly.getTime().size());

        for (int i = 0; i < limit; i++) {
            result.add(WeatherResponse.HourlyWeather.builder()
                    .time(hourly.getTime().get(i))
                    .temperature(hourly.getTemperature2m().get(i))
                    .precipitation(hourly.getPrecipitation().get(i))
                    .build());
        }

        return result;
    }

    /**
     * 캐시된 날씨 데이터
     */
    private static class CachedWeather {
        final WeatherResponse data;
        final long timestamp;

        CachedWeather(WeatherResponse data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
