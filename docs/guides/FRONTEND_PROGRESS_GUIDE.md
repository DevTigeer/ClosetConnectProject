# 옷 업로드 진행도 표시 - 프론트엔드 구현 가이드

## 개요

옷 이미지 업로드 시 AI 처리 진행도를 실시간으로 표시하기 위한 WebSocket 연동 가이드입니다.
사용자는 AI 처리가 진행되는 동안 다른 페이지로 이동할 수 있으며, 진행도는 백그라운드로 표시됩니다.

## 아키텍처

```
[Frontend] ←→ WebSocket ←→ [Spring Boot] ←→ RabbitMQ ←→ [Python AI Worker]
```

1. 사용자가 옷 이미지 업로드
2. Spring Boot가 즉시 응답 반환 (PROCESSING 상태)
3. Python AI Worker가 백그라운드에서 처리 시작
4. Python → RabbitMQ → Spring → WebSocket → Frontend로 진행도 전송
5. 처리 완료 시 READY_FOR_REVIEW 상태로 업데이트

## WebSocket 연결 설정

### 1. 라이브러리 설치

```bash
npm install @stomp/stompjs sockjs-client
```

### 2. WebSocket 연결 코드 (React 예시)

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class ClothProgressService {
  constructor(userId, jwtToken) {
    this.userId = userId;
    this.jwtToken = jwtToken;
    this.client = null;
    this.progressCallbacks = new Map();
  }

  connect() {
    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: {
        Authorization: `Bearer ${this.jwtToken}`
      },
      debug: (str) => {
        console.log('STOMP Debug:', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = (frame) => {
      console.log('✅ WebSocket Connected');

      // 사용자별 진행도 큐 구독
      this.client.subscribe(`/queue/cloth/progress/${this.userId}`, (message) => {
        const progressData = JSON.parse(message.body);
        this.handleProgressUpdate(progressData);
      });
    };

    this.client.onStompError = (frame) => {
      console.error('❌ STOMP Error:', frame);
    };

    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
    }
  }

  // 특정 clothId에 대한 진행도 콜백 등록
  onProgress(clothId, callback) {
    this.progressCallbacks.set(clothId, callback);
  }

  // 진행도 업데이트 핸들러
  handleProgressUpdate(progressData) {
    console.log('📊 Progress Update:', progressData);

    const {
      clothId,
      status,
      currentStep,
      progressPercentage,
      errorMessage
    } = progressData;

    // 등록된 콜백 호출
    const callback = this.progressCallbacks.get(clothId);
    if (callback) {
      callback({
        clothId,
        status,
        step: currentStep,
        percentage: progressPercentage,
        error: errorMessage
      });
    }

    // 완료 또는 실패 시 콜백 제거
    if (status === 'READY_FOR_REVIEW' || status === 'FAILED' || status === 'COMPLETED') {
      this.progressCallbacks.delete(clothId);
    }
  }
}

export default ClothProgressService;
```

### 3. React 컴포넌트에서 사용

```javascript
import React, { useEffect, useState } from 'react';
import ClothProgressService from './ClothProgressService';

function ClothUploadPage() {
  const [progressService, setProgressService] = useState(null);
  const [uploadProgress, setUploadProgress] = useState({});
  const userId = 1; // 로그인한 사용자 ID
  const jwtToken = localStorage.getItem('jwt'); // JWT 토큰

  useEffect(() => {
    // WebSocket 연결
    const service = new ClothProgressService(userId, jwtToken);
    service.connect();
    setProgressService(service);

    return () => {
      service.disconnect();
    };
  }, [userId, jwtToken]);

  const handleFileUpload = async (file) => {
    const formData = new FormData();
    formData.append('imageFile', file);
    formData.append('name', file.name);

    try {
      // 1. 파일 업로드 API 호출
      const response = await fetch(`/api/v1/cloth/upload?userId=${userId}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${jwtToken}`
        },
        body: formData
      });

      const cloth = await response.json();
      const clothId = cloth.id;

      // 2. 진행도 콜백 등록
      progressService.onProgress(clothId, (progress) => {
        console.log(`진행도: ${progress.percentage}% - ${progress.step}`);

        setUploadProgress(prev => ({
          ...prev,
          [clothId]: progress
        }));

        // 완료 처리
        if (progress.status === 'READY_FOR_REVIEW') {
          console.log('✅ 처리 완료! 카테고리 확인 페이지로 이동');
          // TODO: 카테고리 확인 페이지로 리다이렉트
        }

        // 실패 처리
        if (progress.status === 'FAILED') {
          console.error('❌ 처리 실패:', progress.error);
          alert(`AI 처리 실패: ${progress.error}`);
        }
      });

      // 3. 초기 상태 표시
      setUploadProgress(prev => ({
        ...prev,
        [clothId]: {
          percentage: 0,
          step: 'AI 처리 대기 중...',
          status: 'PROCESSING'
        }
      }));

    } catch (error) {
      console.error('업로드 실패:', error);
      alert('업로드에 실패했습니다.');
    }
  };

  return (
    <div>
      <h1>옷 업로드</h1>
      <input type="file" onChange={(e) => handleFileUpload(e.target.files[0])} />

      {/* 진행도 표시 */}
      <div>
        {Object.entries(uploadProgress).map(([clothId, progress]) => (
          <div key={clothId} style={{ marginTop: '20px' }}>
            <p>{progress.step}</p>
            <ProgressBar percentage={progress.percentage} />
            <p>{progress.percentage}%</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function ProgressBar({ percentage }) {
  return (
    <div style={{
      width: '100%',
      height: '20px',
      backgroundColor: '#e0e0e0',
      borderRadius: '10px',
      overflow: 'hidden'
    }}>
      <div style={{
        width: `${percentage}%`,
        height: '100%',
        backgroundColor: '#4caf50',
        transition: 'width 0.3s ease'
      }} />
    </div>
  );
}

export default ClothUploadPage;
```

## 메시지 포맷

### 진행도 메시지 (WebSocket)

```json
{
  "clothId": 123,
  "userId": 1,
  "status": "PROCESSING",
  "currentStep": "배경 제거 중...",
  "progressPercentage": 33,
  "errorMessage": null,
  "timestamp": 1703644800000
}
```

#### 필드 설명

- `clothId`: 옷 ID
- `userId`: 사용자 ID
- `status`: 처리 상태
  - `PROCESSING`: 처리 중
  - `READY_FOR_REVIEW`: 처리 완료 (카테고리 확인 필요)
  - `FAILED`: 처리 실패
  - `COMPLETED`: 최종 완료
- `currentStep`: 현재 진행 단계 메시지
- `progressPercentage`: 진행률 (0-100)
- `errorMessage`: 에러 메시지 (실패 시)
- `timestamp`: 타임스탬프 (밀리초)

### 진행 단계

| 진행률 | 단계 | 설명 |
|-------|------|------|
| 10% | 배경 제거 중... | rembg 처리 시작 |
| 33% | 배경 제거 완료 | rembg 처리 완료 |
| 40% | 옷 영역 분석 중... | 세그멘테이션 시작 |
| 66% | 옷 영역 분석 완료 | 세그멘테이션 완료 |
| 70% | 이미지 복원 중... | 인페인팅 시작 |
| 95% | 이미지 복원 완료 | 인페인팅 완료 |
| 100% | 처리 완료 | READY_FOR_REVIEW 상태 |

## 백그라운드 진행도 표시 (추천 UX)

### 옵션 1: 플로팅 프로그레스 바

화면 하단에 작은 프로그레스 바를 띄워서 사용자가 다른 페이지로 이동해도 진행도를 확인할 수 있게 합니다.

```javascript
function FloatingProgress({ uploads }) {
  return (
    <div style={{
      position: 'fixed',
      bottom: 20,
      right: 20,
      backgroundColor: 'white',
      boxShadow: '0 2px 10px rgba(0,0,0,0.2)',
      borderRadius: '8px',
      padding: '16px',
      width: '300px',
      zIndex: 1000
    }}>
      <h4>AI 처리 중</h4>
      {uploads.map(upload => (
        <div key={upload.clothId}>
          <p>{upload.step}</p>
          <ProgressBar percentage={upload.percentage} />
        </div>
      ))}
    </div>
  );
}
```

### 옵션 2: 알림 목록

헤더에 알림 아이콘을 두고, 클릭하면 진행 중인 업로드 목록을 표시합니다.

### 옵션 3: 전용 진행도 페이지

업로드 후 `/cloth/processing/{clothId}` 페이지로 이동하여 상세 진행도를 표시합니다.

## 테스트

### 1. Spring Boot 실행

```bash
./gradlew bootRun
```

### 2. Python Worker 실행

```bash
cd aiModel
python3 cloth_processing_worker.py
```

### 3. RabbitMQ 실행 (Docker)

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

### 4. 프론트엔드 테스트

1. JWT 토큰으로 로그인
2. WebSocket 연결 확인
3. 파일 업로드
4. 콘솔에서 진행도 메시지 확인

## 주의사항

1. **JWT 토큰**: WebSocket 연결 시 `Authorization` 헤더에 JWT 토큰 필수
2. **사용자 ID**: 로그인한 사용자의 ID를 정확히 전달해야 함
3. **재연결**: WebSocket 연결이 끊어지면 자동 재연결됨 (5초 간격)
4. **메모리 관리**: 완료된 진행도 콜백은 자동으로 제거됨
5. **에러 처리**: `FAILED` 상태 메시지를 받으면 사용자에게 에러 표시

## 참고

- WebSocket 엔드포인트: `ws://localhost:8080/ws` (SockJS)
- 구독 경로: `/queue/cloth/progress/{userId}`
- Spring Security: JWT 인증 필요
