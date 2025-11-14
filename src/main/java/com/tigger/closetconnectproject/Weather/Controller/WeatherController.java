package com.tigger.closetconnectproject.Weather.Controller;

import com.tigger.closetconnectproject.Weather.Dto.CityPreset;
import com.tigger.closetconnectproject.Weather.Dto.ClothingRecommendation;
import com.tigger.closetconnectproject.Weather.Dto.WeatherResponse;
import com.tigger.closetconnectproject.Weather.Service.ClothingRecommendationService;
import com.tigger.closetconnectproject.Weather.Service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 날씨 정보 조회 API Controller
 * - Open-Meteo API 기반
 * - 한국 주요 도시 프리셋 제공
 * - 브라우저 현재 위치 지원
 * - 날씨 기반 옷 추천 기능
 */
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;
    private final ClothingRecommendationService clothingRecommendationService;

    /**
     * 설명: 한국 주요 도시 프리셋 목록 반환
     * @return 도시 프리셋 목록 (서울, 인천, 부산, 대구, 광주, 제주)
     * @example GET /api/v1/weather/cities
     */
    @GetMapping("/cities")
    public CityPreset[] getCityPresets() {
        return weatherService.getCityPresets();
    }

    /**
     * 설명: 특정 위치의 현재 날씨 + 오늘/시간별 예보 조회
     * - 위도/경도로 조회 (브라우저 geolocation 사용)
     * - 10분 캐싱 적용
     * @param latitude 위도 (필수)
     * @param longitude 경도 (필수)
     * @return 현재 날씨 + 오늘 최고/최저 + 시간별 예보 (24시간)
     * @example GET /api/v1/weather/current?latitude=37.57&longitude=126.98
     */
    @GetMapping("/current")
    public WeatherResponse getCurrentWeather(
            @RequestParam Double latitude,
            @RequestParam Double longitude
    ) {
        return weatherService.getCurrentWeather(latitude, longitude);
    }

    /**
     * 설명: 도시 코드로 날씨 조회
     * - 프리셋 도시 버튼용 엔드포인트
     * @param cityCode 도시 코드 (seoul, incheon, busan, daegu, gwangju, jeju)
     * @return 해당 도시의 날씨 정보
     * @example GET /api/v1/weather/city/seoul
     */
    @GetMapping("/city/{cityCode}")
    public WeatherResponse getWeatherByCity(@PathVariable String cityCode) {
        return weatherService.getWeatherByCity(cityCode);
    }

    /**
     * 설명: 서울 날씨 조회 (기본값)
     * - 앱 진입 시 기본 호출용
     * @return 서울의 현재 날씨
     * @example GET /api/v1/weather/default
     */
    @GetMapping("/default")
    public WeatherResponse getDefaultWeather() {
        return weatherService.getWeatherByCity("seoul");
    }

    /**
     * 설명: 도시 코드로 날씨 + 옷 추천 조회
     * - 날씨 정보 + 체감온도 기반 옷 추천 함께 제공
     * @param cityCode 도시 코드 (seoul, incheon, busan, daegu, gwangju, jeju)
     * @return 옷 추천 정보
     * @example GET /api/v1/weather/recommendation/seoul
     */
    @GetMapping("/recommendation/{cityCode}")
    public ClothingRecommendation getRecommendationByCity(@PathVariable String cityCode) {
        // 1. 날씨 정보 조회
        WeatherResponse weather = weatherService.getWeatherByCity(cityCode);

        // 2. 옷 추천 생성
        return clothingRecommendationService.getRecommendation(
                weather.getCurrent().getTemperature(),
                weather.getCurrent().getWeatherDescription(),
                getHumidityFromHourly(weather) // 현재 습도 (시간별 데이터에서 추출)
        );
    }

    /**
     * 설명: 위도/경도로 날씨 + 옷 추천 조회
     * - 현재 위치 기반 옷 추천
     * @param latitude 위도
     * @param longitude 경도
     * @return 옷 추천 정보
     * @example GET /api/v1/weather/recommendation?latitude=37.57&longitude=126.98
     */
    @GetMapping("/recommendation")
    public ClothingRecommendation getRecommendationByLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude
    ) {
        // 1. 날씨 정보 조회
        WeatherResponse weather = weatherService.getCurrentWeather(latitude, longitude);

        // 2. 옷 추천 생성
        return clothingRecommendationService.getRecommendation(
                weather.getCurrent().getTemperature(),
                weather.getCurrent().getWeatherDescription(),
                getHumidityFromHourly(weather)
        );
    }

    /**
     * 설명: 수동 입력값으로 옷 추천 조회 (테스트용)
     * @param temperature 온도
     * @param weatherCondition 날씨 상태
     * @param humidity 습도
     * @return 옷 추천 정보
     * @example GET /api/v1/weather/recommendation/custom?temperature=15&weatherCondition=맑음&humidity=60
     */
    @GetMapping("/recommendation/custom")
    public ClothingRecommendation getCustomRecommendation(
            @RequestParam Double temperature,
            @RequestParam(defaultValue = "맑음") String weatherCondition,
            @RequestParam(defaultValue = "50") Integer humidity
    ) {
        return clothingRecommendationService.getRecommendation(
                temperature,
                weatherCondition,
                humidity
        );
    }

    /**
     * 시간별 데이터에서 현재 시각의 습도 추정
     * (Open-Meteo API는 현재 습도를 직접 제공하지 않으므로, 임시로 50% 반환)
     * TODO: Open-Meteo API에 relative_humidity_2m 파라미터 추가 필요
     */
    private Integer getHumidityFromHourly(WeatherResponse weather) {
        // 임시: 기본값 50% 반환
        // 실제로는 hourly 데이터에서 현재 시각의 습도를 찾아야 함
        return 50;
    }
}
