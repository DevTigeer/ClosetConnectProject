import { useEffect, useState, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { useClothUpload } from '../contexts/ClothUploadContext';
import { clothAPI } from '../services/api';
import ImageSelectionModal from './ImageSelectionModal';
import { getCurrentUserId } from '../utils/authUtils';
import './GlobalProgressTracker.css';

function GlobalProgressTracker() {
  console.log('🟢 GlobalProgressTracker 함수 호출됨');

  const { activeUploads, addUpload, updateProgress, completeUpload, removeUpload } = useClothUpload();
  console.log('🟢 activeUploads from context:', activeUploads);

  // useRef로 최신 activeUploads를 항상 참조 (클로저 문제 해결)
  const activeUploadsRef = useRef(activeUploads);

  const [stompClient, setStompClient] = useState(null);
  const [connected, setConnected] = useState(false);
  const [minimized, setMinimized] = useState(false);
  const [selectedClothForModal, setSelectedClothForModal] = useState(null);
  const [showImageSelectionModal, setShowImageSelectionModal] = useState(false);

  // JWT 토큰에서 현재 로그인한 사용자의 userId 가져오기
  const userId = getCurrentUserId();
  console.log('🔑 현재 로그인한 userId:', userId);

  // 완료된 항목 클릭 핸들러
  const handleCompletedClick = async (upload) => {
    if (upload.status !== 'READY_FOR_REVIEW') {
      return; // 완료 상태가 아니면 무시
    }

    try {
      // 옷 상세 정보 조회
      const response = await clothAPI.getOne(upload.clothId);
      setSelectedClothForModal(response.data);
      setShowImageSelectionModal(true);
    } catch (err) {
      console.error('옷 정보 조회 실패:', err);
      alert('옷 정보를 불러오는데 실패했습니다.');
    }
  };

  // 모달 닫기
  const handleModalClose = () => {
    setShowImageSelectionModal(false);
    setSelectedClothForModal(null);
  };

  // 이미지 확정 완료 콜백
  const handleImageConfirmed = (clothId) => {
    // 확정 완료 후 activeUploads에서 제거
    if (clothId) {
      removeUpload(clothId);
    }

    // 옷장 페이지에서 목록을 새로고침하도록 이벤트 발생 (선택적)
    window.dispatchEvent(new Event('clothConfirmed'));
  };

  // 삭제 버튼 클릭 핸들러
  const handleRemoveUpload = (e, upload) => {
    e.stopPropagation(); // 이벤트 버블링 방지 (완료된 항목 클릭 방지)

    const confirmMessage = upload.status === 'PROCESSING'
      ? `옷 #${upload.clothId}의 AI 처리를 취소하시겠습니까?\n(처리 중인 작업은 계속 진행될 수 있습니다)`
      : `옷 #${upload.clothId}을(를) 목록에서 제거하시겠습니까?`;

    if (window.confirm(confirmMessage)) {
      // markAsDismissed=true로 설정하여 재추가 방지
      removeUpload(upload.clothId, true);
      console.log(`🗑️  사용자가 옷 #${upload.clothId} 제거 (재추가 방지)`);
    }
  };

  // activeUploads가 변경될 때마다 ref 업데이트 (WebSocket 콜백에서 최신값 참조)
  useEffect(() => {
    activeUploadsRef.current = activeUploads;
    console.log('🔄 activeUploadsRef 업데이트:', activeUploads.map(u => u.clothId));
  }, [activeUploads]);

  useEffect(() => {
    if (!userId) {
      console.log('⚠️  userId 없음, WebSocket 연결 건너뜀');
      return;
    }
    console.log('🔵 GlobalProgressTracker useEffect 실행, userId:', userId);
    console.log('🔵 현재 activeUploads:', activeUploads);

    // WebSocket 연결 - 환경변수에서 URL 가져오기
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    const wsUrl = `${baseUrl}/ws`;
    console.log('🔵 WebSocket URL:', wsUrl);

    let socket;
    let client;

    try {
      socket = new SockJS(wsUrl);
      client = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,

        onConnect: () => {
        console.log('✅ WebSocket 연결 성공');
        console.log('✅ 구독 경로:', `/queue/cloth/progress/${userId}`);
        setConnected(true);

        // 진행 상황 구독
        const subscription = client.subscribe(`/queue/cloth/progress/${userId}`, (message) => {
          const data = JSON.parse(message.body);
          console.log('📊 진행 상황 수신:', data);
          console.log('📊 현재 activeUploadsRef에 있는 clothId들:', activeUploadsRef.current.map(u => u.clothId));

          // 현재 추적 중인 작업인지 확인
          const isTracking = activeUploadsRef.current.some(upload => upload.clothId === data.clothId);

          // 추적 중이 아닌 작업이면 자동으로 추가 (타이밍 이슈 해결)
          if (!isTracking) {
            console.log('⚡ 추적 중이 아닌 작업 감지, 자동 추가:', data.clothId);
            // userId가 일치하면 자동으로 activeUploads에 추가
            if (data.userId === userId) {
              addUpload(data.clothId, data.userId);
              console.log('✅ activeUploads에 자동 추가 완료');
            } else {
              console.warn('⚠️  userId 불일치, 무시:', data.userId, 'vs', userId);
              return;
            }
          }

          // 진행도 업데이트
          console.log('🔄 updateProgress 호출:', data.clothId);
          updateProgress(data.clothId, {
            status: data.status,
            currentStep: data.currentStep,
            progressPercentage: data.progressPercentage,
            errorMessage: data.errorMessage
          });
          console.log('✅ updateProgress 완료');

          // 완료 처리
          if (data.status === 'READY_FOR_REVIEW') {
            completeUpload(data.clothId);

            // 완료 알림
            if ('Notification' in window && Notification.permission === 'granted') {
              new Notification('옷 등록 완료!', {
                body: '이미지 처리가 완료되었습니다. 카테고리를 확인해주세요.',
                icon: '/logo.png'
              });
            }
          }

          // 실패 처리
          if (data.status === 'FAILED') {
            alert(`AI 처리 실패: ${data.errorMessage || '알 수 없는 오류'}`);

            // 실패한 항목을 목록에서 제거
            setTimeout(() => {
              removeUpload(data.clothId);
            }, 3000); // 3초 후 제거 (사용자가 메시지를 볼 시간 확보)
          }
        });

        console.log('✅ 구독 완료:', subscription);
      },

      onDisconnect: () => {
        console.log('❌ WebSocket 연결 해제');
        setConnected(false);
      },

      onStompError: (frame) => {
        console.error('❌ WebSocket 오류:', frame);
        console.error('❌ 오류 상세:', frame.headers, frame.body);
        setConnected(false);
      },
      });

      client.activate();
      setStompClient(client);

      // 알림 권한 요청
      if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
      }
    } catch (error) {
      console.error('❌ WebSocket 초기화 실패 (백엔드 서버가 꺼져있을 수 있습니다):', error);
      setConnected(false);
      // 에러가 발생해도 페이지는 정상적으로 로드되어야 함
    }

    return () => {
      try {
        if (client) {
          client.deactivate();
        }
      } catch (error) {
        console.error('❌ WebSocket deactivate 실패:', error);
      }
    };
  }, [userId, addUpload, updateProgress, completeUpload, removeUpload]);

  // 업로드가 없으면 UI만 숨김 (WebSocket은 유지)
  if (activeUploads.length === 0) {
    return null;
  }

  return (
    <>
    <div className={`global-progress-tracker ${minimized ? 'minimized' : ''}`}>
      {/* 헤더 */}
      <div className="tracker-header" onClick={() => setMinimized(!minimized)}>
        <div className="header-left">
          <span className={`status-indicator ${connected ? 'connected' : 'disconnected'}`}></span>
          <h4>AI 처리 중 ({activeUploads.length})</h4>
        </div>
        <button className="minimize-btn">
          {minimized ? '▲' : '▼'}
        </button>
      </div>

      {/* 진행도 리스트 */}
      {!minimized && (
        <div className="tracker-body">
          {activeUploads.map((upload) => (
            <div
              key={upload.clothId}
              className={`upload-item ${upload.status === 'READY_FOR_REVIEW' ? 'clickable' : ''}`}
              onClick={() => handleCompletedClick(upload)}
            >
              <div className="upload-info">
                <div className="upload-info-left">
                  <span className="upload-id">옷 #{upload.clothId}</span>
                  <span className="upload-step">{upload.currentStep}</span>
                </div>
                <button
                  className="remove-upload-btn"
                  onClick={(e) => handleRemoveUpload(e, upload)}
                  title="목록에서 제거"
                >
                  ✕
                </button>
              </div>

              {/* 진행 바 */}
              <div className="progress-bar-container">
                <div
                  className={`progress-bar-fill ${upload.status === 'FAILED' ? 'error' : ''}`}
                  style={{ width: `${upload.progressPercentage || 0}%` }}
                >
                  <span className="progress-percentage">
                    {upload.progressPercentage || 0}%
                  </span>
                </div>
              </div>

              {/* 상태 메시지 */}
              {upload.status === 'READY_FOR_REVIEW' && (
                <div className="status-message success">
                  ✅ 처리 완료! 클릭해서 이미지를 선택하세요.
                </div>
              )}

              {upload.status === 'FAILED' && (
                <div className="status-message error">
                  ❌ 처리 실패: {upload.errorMessage}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>

    {/* 이미지 선택 모달 */}
    {showImageSelectionModal && selectedClothForModal && (
      <ImageSelectionModal
        cloth={selectedClothForModal}
        onClose={handleModalClose}
        onConfirm={handleImageConfirmed}
      />
    )}
    </>
  );
}

export default GlobalProgressTracker;
