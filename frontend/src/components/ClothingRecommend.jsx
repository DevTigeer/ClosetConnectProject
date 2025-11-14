import { useState, useEffect } from 'react';
import { weatherAPI } from '../services/api';
import './ClothingRecommend.css';

function ClothingRecommend() {
  const [recommendation, setRecommendation] = useState(null);
  const [weather, setWeather] = useState(null);
  const [cities, setCities] = useState([]);
  const [selectedCity, setSelectedCity] = useState('seoul');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // 도시 목록 로드
  useEffect(() => {
    loadCities();
  }, []);

  // 기본 추천 로드 (서울)
  useEffect(() => {
    loadRecommendationByCity('seoul');
  }, []);

  const loadCities = async () => {
    try {
      const response = await weatherAPI.getCities();
      setCities(response.data);
    } catch (error) {
      console.error('Failed to load cities:', error);
    }
  };

  const loadRecommendationByCity = async (cityCode) => {
    setLoading(true);
    setError('');
    try {
      // 날씨와 추천 정보를 동시에 가져오기
      const [weatherResponse, recommendationResponse] = await Promise.all([
        weatherAPI.getByCity(cityCode),
        weatherAPI.getRecommendationByCity(cityCode),
      ]);

      setWeather(weatherResponse.data);
      setRecommendation(recommendationResponse.data);
      setSelectedCity(cityCode);
    } catch (error) {
      console.error('Failed to load recommendation:', error);
      setError('추천 정보를 불러올 수 없습니다.');
    } finally {
      setLoading(false);
    }
  };

  const loadRecommendationByLocation = async () => {
    if (!navigator.geolocation) {
      setError('브라우저가 위치 정보를 지원하지 않습니다.');
      return;
    }

    setLoading(true);
    setError('');

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const lat = position.coords.latitude;
        const lon = position.coords.longitude;

        try {
          const [weatherResponse, recommendationResponse] = await Promise.all([
            weatherAPI.getCurrent(lat, lon),
            weatherAPI.getRecommendationByLocation(lat, lon),
          ]);

          setWeather(weatherResponse.data);
          setRecommendation(recommendationResponse.data);
          setSelectedCity(null);
        } catch (error) {
          console.error('Failed to load recommendation:', error);
          setError('추천 정보를 불러올 수 없습니다.');
        } finally {
          setLoading(false);
        }
      },
      (error) => {
        setLoading(false);
        setError('현재 위치를 사용할 수 없어 기본 위치(서울)를 사용합니다.');
        loadRecommendationByCity('seoul');
      }
    );
  };

  if (loading) {
    return (
      <div className="clothing-recommend">
        <div className="loading-message">옷 추천 정보를 불러오는 중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="clothing-recommend">
        <div className="error-message">{error}</div>
      </div>
    );
  }

  if (!recommendation || !weather) {
    return null;
  }

  return (
    <div className="clothing-recommend">
      {/* 위치 선택 */}
      <div className="location-section">
        <h3>📍 위치 선택</h3>
        <div className="city-buttons">
          {cities.map((city) => (
            <button
              key={city.code}
              className={`city-btn ${selectedCity === city.code ? 'active' : ''}`}
              onClick={() => loadRecommendationByCity(city.code)}
            >
              {city.name}
            </button>
          ))}
        </div>
        <button className="current-location-btn" onClick={loadRecommendationByLocation}>
          📍 현재 위치 사용
        </button>
      </div>

      {/* 현재 날씨 요약 */}
      <div className="weather-summary">
        <div className="weather-emoji">{weather.current.weatherEmoji}</div>
        <div className="weather-info">
          <div className="current-temp">{Math.round(weather.current.temperature)}°C</div>
          <div className="weather-description">{weather.current.weatherDescription}</div>
        </div>
      </div>

      {/* 옷 추천 카드 */}
      <div className="recommendation-card">
        <h2>👔 오늘의 옷차림 추천</h2>

        <div className="feels-like">
          체감 온도는 약 {recommendation.feelsLikeTemperature}°C 정도예요
        </div>

        <div className="clothing-items">
          <div className="clothing-item">
            <div className="clothing-label">👕 상의</div>
            <div className="clothing-value">{recommendation.topRecommendation}</div>
          </div>

          <div className="clothing-item">
            <div className="clothing-label">👖 하의</div>
            <div className="clothing-value">{recommendation.bottomRecommendation}</div>
          </div>

          <div className="clothing-item">
            <div className="clothing-label">🎒 추가 아이템</div>
            <div className="clothing-value">{recommendation.extraRecommendation}</div>
          </div>
        </div>

        <div className="recommendation-message">
          {recommendation.recommendationMessage}
        </div>
      </div>
    </div>
  );
}

export default ClothingRecommend;
