import { useState, useEffect } from 'react';
import { weatherAPI } from '../services/api';
import './WeatherRecommend.css';

function WeatherRecommend() {
  const [weather, setWeather] = useState(null);
  const [cities, setCities] = useState([]);
  const [selectedCity, setSelectedCity] = useState('seoul');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    loadCities();
    loadDefaultWeather();
  }, []);

  // 도시 목록 로드
  const loadCities = async () => {
    try {
      const response = await weatherAPI.getCities();
      setCities(response.data || []);
    } catch (err) {
      console.error(err);
    }
  };

  // 기본 날씨 로드
  const loadDefaultWeather = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await weatherAPI.getDefault();
      setWeather(response.data);
    } catch (err) {
      console.error(err);
      setError('날씨 정보를 불러올 수 없습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 도시별 날씨 로드
  const loadWeatherByCity = async (cityCode) => {
    setLoading(true);
    setError('');
    setSelectedCity(cityCode);
    try {
      const response = await weatherAPI.getByCity(cityCode);
      setWeather(response.data);
    } catch (err) {
      console.error(err);
      setError('날씨 정보를 불러올 수 없습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 현재 위치 사용
  const useCurrentLocation = () => {
    if (!navigator.geolocation) {
      setError('브라우저가 위치 정보를 지원하지 않습니다.');
      return;
    }

    setLoading(true);
    setError('');

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const { latitude, longitude } = position.coords;
        try {
          const response = await weatherAPI.getCurrent(latitude, longitude);
          setWeather(response.data);
          setSelectedCity(null);
        } catch (err) {
          console.error(err);
          setError('현재 위치의 날씨를 불러올 수 없습니다.');
          loadDefaultWeather();
        } finally {
          setLoading(false);
        }
      },
      (err) => {
        setLoading(false);
        setError('위치 권한이 필요합니다. 기본 위치를 사용합니다.');
        loadDefaultWeather();
      }
    );
  };

  // 시간 포맷
  const formatTime = (isoTime) => {
    const date = new Date(isoTime);
    return `${date.getHours()}시`;
  };

  if (loading) {
    return (
      <div className="weather-recommend">
        <div className="loading">날씨 정보를 불러오는 중...</div>
      </div>
    );
  }

  return (
    <div className="weather-recommend">
      {error && <div className="weather-error">{error}</div>}

      {/* 도시 선택 */}
      <div className="city-selector">
        <div className="city-buttons">
          {cities.map((city) => (
            <button
              key={city.code}
              className={`city-btn ${selectedCity === city.code ? 'active' : ''}`}
              onClick={() => loadWeatherByCity(city.code)}
            >
              {city.name}
            </button>
          ))}
        </div>
        <button className="location-btn" onClick={useCurrentLocation}>
          📍 현재 위치
        </button>
      </div>

      {/* 현재 날씨 */}
      {weather && (
        <>
          <div className="current-weather">
            <div className="weather-main">
              <div className="weather-emoji">{weather.current.weatherEmoji}</div>
              <div className="weather-temp">{Math.round(weather.current.temperature)}°</div>
            </div>
            <div className="weather-description">{weather.current.weatherDescription}</div>
            <div className="temp-range">
              <span className="temp-max">최고 {Math.round(weather.today.temperatureMax)}°</span>
              <span className="temp-separator">·</span>
              <span className="temp-min">최저 {Math.round(weather.today.temperatureMin)}°</span>
            </div>
          </div>

          {/* 시간별 예보 */}
          <div className="hourly-forecast">
            <h4>시간별 예보</h4>
            <div className="hourly-scroll">
              {weather.hourly.slice(0, 12).map((hour, index) => (
                <div key={index} className="hourly-item">
                  <div className="hourly-time">{formatTime(hour.time)}</div>
                  <div className="hourly-temp">{Math.round(hour.temperature)}°</div>
                  {hour.precipitation > 0 && (
                    <div className="hourly-rain">💧 {hour.precipitation}mm</div>
                  )}
                </div>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}

export default WeatherRecommend;
