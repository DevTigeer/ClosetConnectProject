import { useEffect, useState } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import './ClothProgressTracker.css';

// WebSocket URL 생성: HTTPS 환경에서는 자동으로 HTTPS URL 사용
function getWebSocketUrl() {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
  return `${baseUrl}/ws`;
}

const WEBSOCKET_CONFIG = {
  URL: getWebSocketUrl(),
  RECONNECT_DELAY: 5000,
};

const STATUS_TEXT_MAP = {
  'PROCESSING': '처리 중',
  'READY_FOR_REVIEW': '검토 대기',
  'COMPLETED': '완료',
  'FAILED': '실패'
};

function getStatusText(status) {
  return STATUS_TEXT_MAP[status] || status;
}

function ClothProgressTracker({ userId, clothId, onComplete }) {
  const [progress, setProgress] = useState(null);
  const [isVisible, setIsVisible] = useState(false);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (!userId) return;

    // WebSocket 연결
    const socket = new SockJS(WEBSOCKET_CONFIG.URL);
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: WEBSOCKET_CONFIG.RECONNECT_DELAY,

      onConnect: () => {
        setConnected(true);

        // 진행 상황 구독
        stompClient.subscribe(`/queue/cloth/progress/${userId}`, (message) => {
          const data = JSON.parse(message.body);

          // 현재 업로드한 옷만 표시
          if (clothId && data.clothId === clothId) {
            setProgress(data);
            setIsVisible(true);

            // 완료 시 콜백 호출
            if (data.status === 'READY_FOR_REVIEW' && onComplete) {
              onComplete(data);
            }
          }
        });
      },

      onDisconnect: () => {
        setConnected(false);
      },

      onStompError: (frame) => {
        console.error('WebSocket 오류:', frame.headers.message);
      },
    });

    stompClient.activate();

    return () => {
      stompClient.deactivate();
    };
  }, [userId, clothId, onComplete]);

  if (!progress) return null;

  return (
    <div className="progress-tracker">
      {/* 토글 버튼 */}
      <button
        className="toggle-btn"
        onClick={() => setIsVisible(!isVisible)}
      >
        {isVisible ? '진행 상황 숨기기 ▲' : '진행 상황 보기 ▼'}
      </button>

      {/* 진행 상황 패널 */}
      {isVisible && (
        <div className="progress-panel">
          <div className="connection-status">
            <span className={`status-dot ${connected ? 'connected' : 'disconnected'}`}></span>
            {connected ? '실시간 연결됨' : '연결 안됨'}
          </div>

          <h4>AI 처리 진행 중...</h4>

          {/* 진행바 */}
          <div className="progress-bar-wrapper">
            <div
              className="progress-bar"
              style={{ width: `${progress.progressPercentage || 0}%` }}
            >
              <span className="progress-text">{progress.progressPercentage || 0}%</span>
            </div>
          </div>

          {/* 현재 단계 */}
          <div className="status-info">
            <p><strong>현재 단계:</strong> {progress.currentStep || '-'}</p>
            <p><strong>상태:</strong> {getStatusText(progress.status)}</p>
          </div>

          {/* 완료/실패 메시지 */}
          {progress.status === 'READY_FOR_REVIEW' && (
            <div className="success-message">
              ✅ AI 처리가 완료되었습니다! 카테고리를 확인해주세요.
            </div>
          )}

          {progress.status === 'FAILED' && (
            <div className="error-message">
              ❌ 처리 실패: {progress.errorMessage || '알 수 없는 오류'}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default ClothProgressTracker;
