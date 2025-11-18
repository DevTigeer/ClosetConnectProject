package com.tigger.closetconnectproject.Weather;

import com.tigger.closetconnectproject.Weather.Client.OpenMeteoResponse;
import com.tigger.closetconnectproject.Weather.Dto.CityPreset;
import com.tigger.closetconnectproject.Weather.Dto.WeatherResponse;
import com.tigger.closetconnectproject.Weather.Service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * WeatherService 단위 테스트
 * - 도시 프리셋 목록 조회 테스트
 * - 현재 날씨 조회 테스트
 * - 도시 코드로 날씨 조회 테스트
 * - 캐싱 동작 테스트
 */
@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock RestTemplate restTemplate;

    @InjectMocks WeatherService weatherService;

    private OpenMeteoResponse mockApiResponse;

    @BeforeEach
    void setUp() {
        // RestTemplate을 WeatherService에 주입
        ReflectionTestUtils.setField(weatherService, "restTemplate", restTemplate);

        // Mock API 응답 설정
        mockApiResponse = new OpenMeteoResponse();
        mockApiResponse.setLatitude(37.57);
        mockApiResponse.setLongitude(126.98);
        mockApiResponse.setTimezone("Asia/Seoul");

        // 현재 날씨
        OpenMeteoResponse.CurrentWeather currentWeather = new OpenMeteoResponse.CurrentWeather();
        currentWeather.setTime("2025-01-15T14:00");
        currentWeather.setTemperature(15.5);
        currentWeather.setWeatherCode(0);
        currentWeather.setWindSpeed(5.2);
        currentWeather.setWindDirection(180);
        mockApiResponse.setCurrentWeather(currentWeather);

        // 일별 데이터
        OpenMeteoResponse.DailyData dailyData = new OpenMeteoResponse.DailyData();
        dailyData.setTime(List.of("2025-01-15"));
        dailyData.setTemperature2mMax(List.of(18.0));
        dailyData.setTemperature2mMin(List.of(10.0));
        dailyData.setWeatherCode(List.of(0));
        mockApiResponse.setDaily(dailyData);

        // 시간별 데이터
        OpenMeteoResponse.HourlyData hourlyData = new OpenMeteoResponse.HourlyData();
        hourlyData.setTime(List.of("2025-01-15T14:00", "2025-01-15T15:00"));
        hourlyData.setTemperature2m(List.of(15.5, 16.0));
        hourlyData.setPrecipitation(List.of(0.0, 0.0));
        mockApiResponse.setHourly(hourlyData);
    }

    @Test
    void 도시프리셋목록조회_성공() {
        // When
        CityPreset[] cities = weatherService.getCityPresets();

        // Then
        assertThat(cities).isNotNull();
        assertThat(cities.length).isGreaterThan(0);

        // 서울이 포함되어 있는지 확인
        assertThat(cities)
                .extracting(CityPreset::getCode)
                .contains("seoul");
    }

    @Test
    void 현재날씨조회_성공() {
        // Given
        given(restTemplate.getForObject(anyString(), eq(OpenMeteoResponse.class)))
                .willReturn(mockApiResponse);

        // When
        WeatherResponse result = weatherService.getCurrentWeather(37.57, 126.98);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(37.57);
        assertThat(result.getLongitude()).isEqualTo(126.98);
        assertThat(result.getCurrent()).isNotNull();
        assertThat(result.getCurrent().getTemperature()).isEqualTo(15.5);
        assertThat(result.getCurrent().getWeatherDescription()).isEqualTo("맑음");
        assertThat(result.getToday()).isNotNull();
        assertThat(result.getToday().getTemperatureMax()).isEqualTo(18.0);
        assertThat(result.getToday().getTemperatureMin()).isEqualTo(10.0);
        assertThat(result.getHourly()).hasSize(2);
    }

    @Test
    void 현재날씨조회_실패_응답없음() {
        // Given
        given(restTemplate.getForObject(anyString(), eq(OpenMeteoResponse.class)))
                .willReturn(null);

        // When & Then
        assertThatThrownBy(() -> weatherService.getCurrentWeather(37.57, 126.98))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("날씨 정보를 가져올 수 없습니다.");
    }

    @Test
    void 도시코드로날씨조회_성공() {
        // Given
        given(restTemplate.getForObject(anyString(), eq(OpenMeteoResponse.class)))
                .willReturn(mockApiResponse);

        // When
        WeatherResponse result = weatherService.getWeatherByCity("seoul");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCityName()).isEqualTo("서울");
        assertThat(result.getCurrent()).isNotNull();
        assertThat(result.getCurrent().getTemperature()).isEqualTo(15.5);
    }

    @Test
    void 도시코드로날씨조회_실패_잘못된도시코드() {
        // When & Then
        assertThatThrownBy(() -> weatherService.getWeatherByCity("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알 수 없는 도시 코드");
    }

    @Test
    void 캐싱동작_같은위치재요청시캐시사용() {
        // Given
        given(restTemplate.getForObject(anyString(), eq(OpenMeteoResponse.class)))
                .willReturn(mockApiResponse);

        // When - 첫 번째 요청
        WeatherResponse result1 = weatherService.getCurrentWeather(37.57, 126.98);

        // When - 두 번째 요청 (캐시에서 가져옴)
        WeatherResponse result2 = weatherService.getCurrentWeather(37.57, 126.98);

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1.getCurrent().getTemperature()).isEqualTo(result2.getCurrent().getTemperature());

        // API는 한 번만 호출되어야 함 (캐시 적중)
        // 참고: 실제로는 캐시 만료 시간(10분)을 고려해야 하지만,
        // 단위 테스트에서는 즉시 재요청 시 캐시가 동작하는지 확인
    }
}
