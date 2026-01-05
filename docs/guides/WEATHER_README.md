# 🌤️ 날씨 API 가이드

## 개요

Open-Meteo API를 사용한 한국 중심 날씨 정보 서비스입니다.

### 주요 기능

- ✅ **한국 주요 도시 프리셋** (서울, 인천, 부산, 대구, 광주, 제주)
- ✅ **브라우저 현재 위치 지원** (Geolocation API)
- ✅ **실시간 날씨 정보** (온도, 습도, 날씨 상태)
- ✅ **시간별 예보** (24시간, 강수량 포함)
- ✅ **10분 캐싱** (API 호출 최적화)
- ✅ **API 키 불필요** (Open-Meteo 무료 서비스)

---

## 📂 디렉토리 구조

```
Weather/
├── Controller/
│   └── WeatherController.java       # REST API 엔드포인트
├── Service/
│   └── WeatherService.java          # 비즈니스 로직 + 캐싱
├── Dto/
│   ├── CityPreset.java              # 한국 도시 프리셋
│   ├── WeatherCode.java             # 날씨 코드 → 한국어 변환
│   └── WeatherResponse.java         # API 응답 DTO
└── Client/
    └── OpenMeteoResponse.java       # Open-Meteo API 응답 매핑
```

---

## 🌐 API 엔드포인트

### 1. 도시 프리셋 목록 조회

```http
GET /api/v1/weather/cities
```

**응답 예시:**
```json
[
  {
    "code": "seoul",
    "name": "서울",
    "latitude": 37.57,
    "longitude": 126.98
  },
  {
    "code": "busan",
    "name": "부산",
    "latitude": 35.18,
    "longitude": 129.07
  }
]
```

---

### 2. 도시 코드로 날씨 조회

```http
GET /api/v1/weather/city/{cityCode}
```

**파라미터:**
- `cityCode`: 도시 코드 (seoul, incheon, busan, daegu, gwangju, jeju)

**예시:**
```http
GET /api/v1/weather/city/seoul
```

**응답 예시:**
```json
{
  "cityName": "서울",
  "latitude": 37.57,
  "longitude": 126.98,
  "current": {
    "temperature": 15.2,
    "weatherCode": 0,
    "weatherDescription": "맑음",
    "weatherEmoji": "☀️",
    "windSpeed": 5.4,
    "windDirection": 180,
    "time": "2024-01-15T14:00"
  },
  "today": {
    "date": "2024-01-15",
    "temperatureMax": 18.5,
    "temperatureMin": 8.2,
    "weatherCode": 0,
    "weatherDescription": "맑음"
  },
  "hourly": [
    {
      "time": "2024-01-15T00:00",
      "temperature": 10.2,
      "precipitation": 0.0
    },
    // ... 24시간 데이터
  ]
}
```

---

### 3. 위도/경도로 날씨 조회

```http
GET /api/v1/weather/current?latitude={lat}&longitude={lon}
```

**파라미터:**
- `latitude`: 위도 (필수)
- `longitude`: 경도 (필수)

**예시:**
```http
GET /api/v1/weather/current?latitude=37.57&longitude=126.98
```

**응답:** 위와 동일 (cityName 없음)

---

### 4. 기본 날씨 조회 (서울)

```http
GET /api/v1/weather/default
```

**응답:** 서울의 현재 날씨 (위와 동일)

---

## 🎨 프론트엔드 UI

### 접속 방법

```
http://localhost:8080/weather.html
```

### 주요 기능

1. **도시 버튼**
   - 서울, 인천, 부산, 대구, 광주, 제주 버튼 클릭
   - 해당 도시의 날씨 즉시 조회

2. **현재 위치 사용**
   - "현재 위치 사용" 버튼 클릭
   - 브라우저 위치 권한 허용
   - GPS 좌표 기반 날씨 조회

3. **화면 구성**
   - 현재 기온 + 날씨 상태 (이모지 포함)
   - 오늘 최고/최저 기온
   - 시간별 예보 (24시간)
   - 시간별 강수량

---

## 🔧 기술 스택

### 백엔드
- **API**: Open-Meteo (https://open-meteo.com)
- **캐싱**: ConcurrentHashMap (10분 TTL)
- **HTTP 클라이언트**: RestTemplate

### 프론트엔드
- **순수 HTML/CSS/JavaScript** (라이브러리 없음)
- **Geolocation API**: 브라우저 현재 위치
- **Fetch API**: 비동기 HTTP 요청

---

## 🌍 Weather Code 매핑표

| Code | 영문 | 한글 | 이모지 |
|------|------|------|--------|
| 0 | Clear | 맑음 | ☀️ |
| 1 | Mainly Clear | 대체로 맑음 | 🌤️ |
| 2 | Partly Cloudy | 구름 조금 | ⛅ |
| 3 | Overcast | 흐림 | ☁️ |
| 45 | Fog | 안개 | 🌫️ |
| 51-55 | Drizzle | 이슬비 | 🌦️ |
| 61-65 | Rain | 비 | 🌧️ |
| 71-75 | Snow | 눈 | ❄️ |
| 80-82 | Showers | 소나기 | 🌧️ |
| 95-99 | Thunderstorm | 천둥번개 | ⛈️ |

전체 코드는 `WeatherCode.java` 참고

---

## 📝 사용 예시

### Java (Service 호출)

```java
@Autowired
private WeatherService weatherService;

// 서울 날씨 조회
WeatherResponse weather = weatherService.getWeatherByCity("seoul");
System.out.println("현재 온도: " + weather.getCurrent().getTemperature() + "°C");

// 현재 위치 날씨 조회
WeatherResponse current = weatherService.getCurrentWeather(37.57, 126.98);
```

### JavaScript (프론트엔드)

```javascript
// 도시 코드로 조회
const response = await fetch('/api/v1/weather/city/seoul');
const data = await response.json();
console.log('현재 온도:', data.current.temperature);

// 위도/경도로 조회
const response2 = await fetch('/api/v1/weather/current?latitude=37.57&longitude=126.98');
const data2 = await response2.json();
```

---

## ⚙️ 설정

### application.properties

별도 설정 불필요 (API 키 없음)

### SecurityConfig

`/api/v1/weather/**` 경로는 이미 public으로 설정됨

---

## 🚀 실행 방법

1. **서버 시작**
   ```bash
   ./gradlew bootRun
   ```

2. **브라우저 접속**
   ```
   http://localhost:8080/weather.html
   ```

3. **API 테스트**
   ```bash
   curl http://localhost:8080/api/v1/weather/default
   ```

---

## 💡 확장 가능한 기능 (선택)

- [ ] 주간 예보 (7일)
- [ ] 사용자별 선호 위치 저장 (DB)
- [ ] 날씨 알림 (특정 조건 만족 시)
- [ ] 옷 추천 로직 연동 (온도 기반)
- [ ] Redis 캐싱 (분산 환경)

---

## 📞 문의

날씨 API 관련 문의는 프로젝트 이슈로 등록해주세요.
