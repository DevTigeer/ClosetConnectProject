package com.tigger.closetconnectproject.Weather;

import com.tigger.closetconnectproject.Weather.Dto.ClothingRecommendation;
import com.tigger.closetconnectproject.Weather.Service.ClothingRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ClothingRecommendationService 단위 테스트
 * - 온도별 옷 추천 테스트
 * - 날씨 상태별 추가 아이템 추천 테스트
 * - 체감온도 계산 테스트
 */
class ClothingRecommendationServiceTest {

    private ClothingRecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new ClothingRecommendationService();
    }

    @Test
    void 한여름날씨_반팔추천() {
        // Given
        Double temperature = 28.0;
        String weatherCondition = "맑음";
        Integer humidity = 60;

        // When
        ClothingRecommendation result = recommendationService.getRecommendation(
                temperature, weatherCondition, humidity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrentTemperature()).isEqualTo(28.0);
        assertThat(result.getTopRecommendation()).contains("반팔");
        assertThat(result.getRecommendationMessage()).contains("28.0°C");
        assertThat(result.getRecommendationMessage()).contains("맑음");
    }

    @Test
    void 초봄날씨_가디건추천() {
        // Given
        Double temperature = 15.0;
        String weatherCondition = "흐림";
        Integer humidity = 50;

        // When
        ClothingRecommendation result = recommendationService.getRecommendation(
                temperature, weatherCondition, humidity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrentTemperature()).isEqualTo(15.0);
        assertThat(result.getExtraRecommendation()).contains("가디건");
        assertThat(result.getBottomRecommendation()).contains("청바지");
    }

    @Test
    void 한겨울날씨_패딩추천() {
        // Given
        Double temperature = -5.0;
        String weatherCondition = "맑음";
        Integer humidity = 40;

        // When
        ClothingRecommendation result = recommendationService.getRecommendation(
                temperature, weatherCondition, humidity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrentTemperature()).isEqualTo(-5.0);
        assertThat(result.getExtraRecommendation()).contains("패딩");
        assertThat(result.getExtraRecommendation()).contains("목도리");
    }

    @Test
    void 비오는날씨_우산추천() {
        // Given
        Double temperature = 18.0;
        String weatherCondition = "비";
        Integer humidity = 80;

        // When
        ClothingRecommendation result = recommendationService.getRecommendation(
                temperature, weatherCondition, humidity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getExtraRecommendation()).contains("우산");
        assertThat(result.getExtraRecommendation()).contains("우비");
        assertThat(result.getExtraRecommendation()).contains("방수");
    }

    @Test
    void 눈오는날씨_방한용품추천() {
        // Given
        Double temperature = -2.0;
        String weatherCondition = "눈";
        Integer humidity = 70;

        // When
        ClothingRecommendation result = recommendationService.getRecommendation(
                temperature, weatherCondition, humidity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getExtraRecommendation()).contains("방한용품");
        assertThat(result.getExtraRecommendation()).contains("미끄럼 방지 신발");
    }

    @Test
    void 높은습도_통풍옷추천() {
        // Given
        Double temperature = 25.0;
        String weatherCondition = "맑음";
        Integer humidity = 80;

        // When
        ClothingRecommendation result = recommendationService.getRecommendation(
                temperature, weatherCondition, humidity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getExtraRecommendation()).contains("통풍");
        assertThat(result.getHumidity()).isEqualTo(80);
    }

    @Test
    void 체감온도계산_맑은날씨_1도증가() {
        // Given
        Double temperature = 20.0;
        String weatherCondition = "맑음";
        Integer humidity = 50;

        // When
        ClothingRecommendation result = recommendationService.getRecommendation(
                temperature, weatherCondition, humidity);

        // Then
        assertThat(result).isNotNull();
        // 맑음 +1도 체감온도 증가
        assertThat(result.getFeelsLikeTemperature()).isEqualTo(21.0);
    }

    @Test
    void 체감온도계산_높은습도_2도증가() {
        // Given
        Double temperature = 25.0;
        String weatherCondition = "흐림";
        Integer humidity = 75;

        // When
        ClothingRecommendation result = recommendationService.getRecommendation(
                temperature, weatherCondition, humidity);

        // Then
        assertThat(result).isNotNull();
        // 습도 70% 이상 +2도 체감온도 증가
        assertThat(result.getFeelsLikeTemperature()).isEqualTo(27.0);
    }

    @Test
    void 체감온도계산_맑은날씨와높은습도_3도증가() {
        // Given
        Double temperature = 22.0;
        String weatherCondition = "맑음";
        Integer humidity = 80;

        // When
        ClothingRecommendation result = recommendationService.getRecommendation(
                temperature, weatherCondition, humidity);

        // Then
        assertThat(result).isNotNull();
        // 맑음 +1도 + 습도 높음 +2도 = +3도
        assertThat(result.getFeelsLikeTemperature()).isEqualTo(25.0);
    }

    @Test
    void 추천메시지형식_확인() {
        // Given
        Double temperature = 15.0;
        String weatherCondition = "맑음";
        Integer humidity = 50;

        // When
        ClothingRecommendation result = recommendationService.getRecommendation(
                temperature, weatherCondition, humidity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecommendationMessage()).isNotBlank();
        assertThat(result.getRecommendationMessage()).contains("상의:");
        assertThat(result.getRecommendationMessage()).contains("하의:");
        assertThat(result.getRecommendationMessage()).contains("추가:");
        assertThat(result.getRecommendationMessage()).contains("체감 온도");
    }

    @Test
    void 영하날씨_겨울옷추천() {
        // Given
        Double temperature = -10.0;
        String weatherCondition = "맑음";
        Integer humidity = 30;

        // When
        ClothingRecommendation result = recommendationService.getRecommendation(
                temperature, weatherCondition, humidity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getExtraRecommendation()).contains("패딩");
        assertThat(result.getBottomRecommendation()).contains("기모");
    }

    @Test
    void 초여름날씨_얇은옷추천() {
        // Given
        Double temperature = 23.0;
        String weatherCondition = "맑음";
        Integer humidity = 55;

        // When
        ClothingRecommendation result = recommendationService.getRecommendation(
                temperature, weatherCondition, humidity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTopRecommendation()).contains("반팔");
        assertThat(result.getBottomRecommendation()).contains("면바지");
    }
}
