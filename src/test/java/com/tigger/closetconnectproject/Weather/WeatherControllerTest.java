package com.tigger.closetconnectproject.Weather;

import com.tigger.closetconnectproject.Weather.Controller.WeatherController;
import com.tigger.closetconnectproject.Weather.Dto.CityPreset;
import com.tigger.closetconnectproject.Weather.Dto.ClothingRecommendation;
import com.tigger.closetconnectproject.Weather.Dto.WeatherResponse;
import com.tigger.closetconnectproject.Weather.Service.ClothingRecommendationService;
import com.tigger.closetconnectproject.Weather.Service.WeatherService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WeatherController 단위 테스트
 * - 도시 프리셋 목록 조회 테스트
 * - 현재 날씨 조회 테스트
 * - 도시 코드로 날씨 조회 테스트
 * - 기본 날씨 조회 테스트
 * - 옷 추천 조회 테스트
 */
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = WeatherController.class)
class WeatherControllerTest {

    @Autowired MockMvc mvc;

    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockBean AuditorAware<Long> auditorAware;
    @MockBean WeatherService weatherService;
    @MockBean ClothingRecommendationService clothingRecommendationService;

    private WeatherResponse weatherResponse;
    private ClothingRecommendation clothingRecommendation;

    @BeforeEach
    void setUp() {
        weatherResponse = WeatherResponse.builder()
                .cityName("서울")
                .latitude(37.57)
                .longitude(126.98)
                .current(WeatherResponse.CurrentWeather.builder()
                        .temperature(15.5)
                        .weatherCode(0)
                        .weatherDescription("맑음")
                        .weatherEmoji("☀️")
                        .windSpeed(5.2)
                        .windDirection(180)
                        .time("2025-01-15T14:00")
                        .build())
                .today(WeatherResponse.DailyWeather.builder()
                        .date("2025-01-15")
                        .temperatureMax(18.0)
                        .temperatureMin(10.0)
                        .weatherCode(0)
                        .weatherDescription("맑음")
                        .build())
                .hourly(List.of(
                        WeatherResponse.HourlyWeather.builder()
                                .time("2025-01-15T14:00")
                                .temperature(15.5)
                                .precipitation(0.0)
                                .build(),
                        WeatherResponse.HourlyWeather.builder()
                                .time("2025-01-15T15:00")
                                .temperature(16.0)
                                .precipitation(0.0)
                                .build()
                ))
                .build();

        clothingRecommendation = ClothingRecommendation.builder()
                .currentTemperature(15.5)
                .feelsLikeTemperature(16.5)
                .weatherCondition("맑음")
                .humidity(50)
                .topRecommendation("가디건, 니트, 후드티, 맨투맨")
                .bottomRecommendation("청바지, 면바지")
                .extraRecommendation("얇은 외투")
                .recommendationMessage("오늘은 15.5°C 로 맑음 날씨예요.\n" +
                        "체감 온도는 약 16.5°C 정도예요.\n\n" +
                        "추천 코디는 다음과 같아요:\n" +
                        "- 상의: 가디건, 니트, 후드티, 맨투맨\n" +
                        "- 하의: 청바지, 면바지\n" +
                        "- 추가: 얇은 외투\n\n" +
                        "오늘 날씨에 잘 맞는 스타일로 하루를 보내세요!")
                .build();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(weatherService, clothingRecommendationService);
        Mockito.reset(weatherService, clothingRecommendationService);
    }

    @Test
    void 도시프리셋목록조회_성공_200() throws Exception {
        // Given
        CityPreset[] cities = {
                new CityPreset("seoul", "서울", 37.57, 126.98),
                new CityPreset("busan", "부산", 35.18, 129.08)
        };
        given(weatherService.getCityPresets()).willReturn(cities);

        // When & Then
        mvc.perform(get("/api/v1/weather/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code").value("seoul"))
                .andExpect(jsonPath("$[0].name").value("서울"))
                .andExpect(jsonPath("$[1].code").value("busan"))
                .andExpect(jsonPath("$[1].name").value("부산"));

        verify(weatherService).getCityPresets();
    }

    @Test
    void 현재날씨조회_성공_200() throws Exception {
        // Given
        given(weatherService.getCurrentWeather(eq(37.57), eq(126.98)))
                .willReturn(weatherResponse);

        // When & Then
        mvc.perform(get("/api/v1/weather/current")
                        .param("latitude", "37.57")
                        .param("longitude", "126.98"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityName").value("서울"))
                .andExpect(jsonPath("$.latitude").value(37.57))
                .andExpect(jsonPath("$.longitude").value(126.98))
                .andExpect(jsonPath("$.current.temperature").value(15.5))
                .andExpect(jsonPath("$.current.weatherDescription").value("맑음"))
                .andExpect(jsonPath("$.today.temperatureMax").value(18.0))
                .andExpect(jsonPath("$.today.temperatureMin").value(10.0))
                .andExpect(jsonPath("$.hourly", hasSize(2)));

        verify(weatherService).getCurrentWeather(eq(37.57), eq(126.98));
    }

    @Test
    void 도시코드로날씨조회_성공_200() throws Exception {
        // Given
        given(weatherService.getWeatherByCity("seoul"))
                .willReturn(weatherResponse);

        // When & Then
        mvc.perform(get("/api/v1/weather/city/{cityCode}", "seoul"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityName").value("서울"))
                .andExpect(jsonPath("$.current.temperature").value(15.5))
                .andExpect(jsonPath("$.current.weatherDescription").value("맑음"));

        verify(weatherService).getWeatherByCity("seoul");
    }

    @Test
    void 기본날씨조회_성공_200() throws Exception {
        // Given
        given(weatherService.getWeatherByCity("seoul"))
                .willReturn(weatherResponse);

        // When & Then
        mvc.perform(get("/api/v1/weather/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityName").value("서울"))
                .andExpect(jsonPath("$.current.temperature").value(15.5));

        verify(weatherService).getWeatherByCity("seoul");
    }

    @Test
    void 도시코드로옷추천조회_성공_200() throws Exception {
        // Given
        given(weatherService.getWeatherByCity("seoul"))
                .willReturn(weatherResponse);
        given(clothingRecommendationService.getRecommendation(eq(15.5), eq("맑음"), eq(50)))
                .willReturn(clothingRecommendation);

        // When & Then
        mvc.perform(get("/api/v1/weather/recommendation/{cityCode}", "seoul"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTemperature").value(15.5))
                .andExpect(jsonPath("$.feelsLikeTemperature").value(16.5))
                .andExpect(jsonPath("$.weatherCondition").value("맑음"))
                .andExpect(jsonPath("$.topRecommendation").value("가디건, 니트, 후드티, 맨투맨"))
                .andExpect(jsonPath("$.bottomRecommendation").value("청바지, 면바지"))
                .andExpect(jsonPath("$.extraRecommendation").value("얇은 외투"));

        verify(weatherService).getWeatherByCity("seoul");
        verify(clothingRecommendationService).getRecommendation(eq(15.5), eq("맑음"), eq(50));
    }

    @Test
    void 위도경도로옷추천조회_성공_200() throws Exception {
        // Given
        given(weatherService.getCurrentWeather(eq(37.57), eq(126.98)))
                .willReturn(weatherResponse);
        given(clothingRecommendationService.getRecommendation(eq(15.5), eq("맑음"), eq(50)))
                .willReturn(clothingRecommendation);

        // When & Then
        mvc.perform(get("/api/v1/weather/recommendation")
                        .param("latitude", "37.57")
                        .param("longitude", "126.98"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTemperature").value(15.5))
                .andExpect(jsonPath("$.feelsLikeTemperature").value(16.5))
                .andExpect(jsonPath("$.weatherCondition").value("맑음"))
                .andExpect(jsonPath("$.topRecommendation").value("가디건, 니트, 후드티, 맨투맨"));

        verify(weatherService).getCurrentWeather(eq(37.57), eq(126.98));
        verify(clothingRecommendationService).getRecommendation(eq(15.5), eq("맑음"), eq(50));
    }

    @Test
    void 수동입력으로옷추천조회_성공_200() throws Exception {
        // Given
        given(clothingRecommendationService.getRecommendation(eq(15.0), eq("맑음"), eq(60)))
                .willReturn(clothingRecommendation);

        // When & Then
        mvc.perform(get("/api/v1/weather/recommendation/custom")
                        .param("temperature", "15.0")
                        .param("weatherCondition", "맑음")
                        .param("humidity", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTemperature").value(15.5))
                .andExpect(jsonPath("$.weatherCondition").value("맑음"));

        verify(clothingRecommendationService).getRecommendation(eq(15.0), eq("맑음"), eq(60));
    }
}
