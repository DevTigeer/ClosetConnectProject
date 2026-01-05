package com.tigger.closetconnectproject.Weather.Service;

import com.tigger.closetconnectproject.Weather.Dto.ClothingRecommendation;
import com.tigger.closetconnectproject.Weather.Dto.TemperatureRange;
import org.springframework.stereotype.Service;

/**
 * 날씨 기반 옷 추천 Service
 * - 온도, 날씨 상태, 습도를 기반으로 적절한 옷차림 추천
 */
@Service
public class ClothingRecommendationService {

    // 체감 온도 조정 상수
    private static final double SUNNY_TEMP_ADJUSTMENT = 1.0;
    private static final double HIGH_HUMIDITY_TEMP_ADJUSTMENT = 2.0;

    // 습도 임계값
    private static final int HIGH_HUMIDITY_THRESHOLD = 70;

    // 추가 아이템 추천 상수
    private static final String RAIN_ITEMS = ", 우산, 우비/방수 재킷, 방수 신발";
    private static final String SNOW_ITEMS = ", 방한용품, 미끄럼 방지 신발";
    private static final String THUNDERSTORM_WARNING = ", ⚠️ 외출 시 주의 필요";
    private static final String BREATHABLE_ITEMS = ", 통풍 잘되는 소재 추천";

    // 메시지 템플릿
    private static final String RECOMMENDATION_MESSAGE_TEMPLATE =
            "오늘은 %.1f°C 로 %s 날씨예요.\n" +
            "체감 온도는 약 %.1f°C 정도예요.\n\n" +
            "추천 코디는 다음과 같아요:\n" +
            "- 상의: %s\n" +
            "- 하의: %s\n" +
            "- 추가: %s\n\n" +
            "오늘 날씨에 잘 맞는 스타일로 하루를 보내세요!";

    /**
     * 날씨 상태 키워드를 정의하는 Enum
     */
    private enum WeatherKeyword {
        CLEAR("맑음", "clear"),
        RAIN("비", "rain", "소나기", "drizzle"),
        SNOW("눈", "snow"),
        THUNDERSTORM("천둥", "thunderstorm");

        private final String[] keywords;

        WeatherKeyword(String... keywords) {
            this.keywords = keywords;
        }

        public boolean matches(String condition) {
            if (condition == null) return false;
            String lowerCondition = condition.toLowerCase();
            for (String keyword : keywords) {
                if (lowerCondition.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 설명: 날씨 정보를 기반으로 옷 추천 생성
     *
     * 1. 체감온도 계산 (날씨 상태 + 습도 보정)
     * 2. 온도 구간별 기본 추천 결정
     * 3. 날씨 상태에 따른 추가 아이템 보정
     * 4. 최종 추천 문장 생성
     *
     * @param temperature 현재 기온 (°C)
     * @param weatherCondition 날씨 상태 (맑음, 흐림, 비, 눈 등)
     * @param humidity 습도 (%)
     * @return 옷 추천 정보
     */
    public ClothingRecommendation getRecommendation(
            Double temperature,
            String weatherCondition,
            Integer humidity
    ) {
        // 1단계: 체감온도 계산
        double feelsLikeTemp = calculateFeelsLikeTemperature(temperature, weatherCondition, humidity);

        // 2단계: 온도 구간별 기본 추천 가져오기
        TemperatureRange range = TemperatureRange.fromTemperature(feelsLikeTemp);

        // 3단계: 날씨 상태에 따른 추가 아이템 보정
        String extraRecommendation = adjustRecommendationByWeather(
                range.getExtraRecommendation(),
                weatherCondition,
                humidity
        );

        // 4단계: 최종 추천 문장 생성
        String message = generateRecommendationMessage(
                temperature,
                feelsLikeTemp,
                weatherCondition,
                range.getTopRecommendation(),
                range.getBottomRecommendation(),
                extraRecommendation
        );

        return ClothingRecommendation.builder()
                .currentTemperature(temperature)
                .feelsLikeTemperature(Math.round(feelsLikeTemp * 10) / 10.0) // 소수점 1자리
                .weatherCondition(weatherCondition)
                .humidity(humidity)
                .topRecommendation(range.getTopRecommendation())
                .bottomRecommendation(range.getBottomRecommendation())
                .extraRecommendation(extraRecommendation)
                .recommendationMessage(message)
                .build();
    }

    /**
     * 체감온도 계산
     *
     * 보정 규칙:
     * - 맑음: +1°C (햇빛으로 체감 따뜻)
     * - 비/소나기: 변화 없음 (우산/우비 추천으로 대체)
     * - 흐림: 변화 없음
     * - 습도 70% 이상: +2°C (불쾌지수 증가)
     *
     * @param temperature 현재 온도
     * @param weatherCondition 날씨 상태
     * @param humidity 습도
     * @return 체감 온도
     */
    private double calculateFeelsLikeTemperature(
            Double temperature,
            String weatherCondition,
            Integer humidity
    ) {
        double adjustment = 0.0;

        // 날씨 상태 보정
        if (WeatherKeyword.CLEAR.matches(weatherCondition)) {
            adjustment += SUNNY_TEMP_ADJUSTMENT; // 햇빛으로 체감온도 증가
        }
        // 비/눈은 체감온도 변화 없음 (옷 추천에서 처리)

        // 습도 보정
        if (humidity != null && humidity >= HIGH_HUMIDITY_THRESHOLD) {
            adjustment += HIGH_HUMIDITY_TEMP_ADJUSTMENT; // 높은 습도는 더 덥게 느껴짐
        }

        return temperature + adjustment;
    }

    /**
     * 날씨 상태에 따른 추가 아이템 보정
     *
     * - 비/소나기: 우산, 우비, 방수 재킷 추가
     * - 눈: 방한용품, 미끄럼 방지 신발 추가
     * - 습도 높음: 통풍 잘되는 옷 추가
     *
     * @param baseExtra 기본 추가 아이템
     * @param weatherCondition 날씨 상태
     * @param humidity 습도
     * @return 보정된 추가 아이템
     */
    private String adjustRecommendationByWeather(
            String baseExtra,
            String weatherCondition,
            Integer humidity
    ) {
        StringBuilder extra = new StringBuilder(baseExtra);

        // 비/소나기 → 우산, 우비, 방수 재킷 추가
        if (WeatherKeyword.RAIN.matches(weatherCondition)) {
            extra.append(RAIN_ITEMS);
        }

        // 눈 → 방한용품 강화
        if (WeatherKeyword.SNOW.matches(weatherCondition)) {
            extra.append(SNOW_ITEMS);
        }

        // 천둥번개 → 실내 활동 권장
        if (WeatherKeyword.THUNDERSTORM.matches(weatherCondition)) {
            extra.append(THUNDERSTORM_WARNING);
        }

        // 습도 70% 이상 → 통풍 잘되는 옷 추천
        if (humidity != null && humidity >= HIGH_HUMIDITY_THRESHOLD) {
            extra.append(BREATHABLE_ITEMS);
        }

        return extra.toString();
    }

    /**
     * 최종 추천 문장 생성
     *
     * 형식:
     * 오늘은 {현재온도}°C 로 {날씨상태} 날씨예요.
     * 체감 온도는 약 {체감온도}°C 정도예요.
     *
     * 추천 코디는 다음과 같아요:
     * - 상의: {상의추천}
     * - 하의: {하의추천}
     * - 추가: {추가추천}
     *
     * 오늘 날씨에 잘 맞는 스타일로 하루를 보내세요!
     */
    private String generateRecommendationMessage(
            Double temperature,
            Double feelsLikeTemp,
            String weatherCondition,
            String topRec,
            String bottomRec,
            String extraRec
    ) {
        return String.format(
            RECOMMENDATION_MESSAGE_TEMPLATE,
            temperature,
            weatherCondition,
            feelsLikeTemp,
            topRec,
            bottomRec,
            extraRec
        );
    }
}
