import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { isTokenExpired, decodeJWT } from '../utils/authUtils';
import { userAPI } from '../services/api';
import './TokenExpiryWarning.css';

/**
 * JWT 토큰 만료 경고 컴포넌트
 * - 토큰 만료 5분 전에 경고 모달 표시
 * - "계속 사용하기" 클릭 시 토큰 갱신 (API 호출로 활성화)
 * - 토큰 완전 만료 시 자동 로그아웃
 */
function TokenExpiryWarning() {
  const [showWarning, setShowWarning] = useState(false);
  const [timeRemaining, setTimeRemaining] = useState(null);
  const navigate = useNavigate();

  // 토큰 상태 확인
  const checkTokenExpiry = useCallback(() => {
    const token = localStorage.getItem('accessToken');

    if (!token) {
      return { status: 'none' };
    }

    if (isTokenExpired(token)) {
      return { status: 'expired' };
    }

    // 남은 시간 계산
    const payload = decodeJWT(token);
    if (!payload || !payload.exp) {
      return { status: 'invalid' };
    }

    const now = Math.floor(Date.now() / 1000);
    const timeLeft = payload.exp - now;

    // 5분 = 300초
    const WARNING_THRESHOLD = 300;

    if (timeLeft <= WARNING_THRESHOLD && timeLeft > 0) {
      return { status: 'expiring', timeLeft };
    }

    return { status: 'valid', timeLeft };
  }, []);

  // 토큰 갱신 (API 호출로 활성화)
  const handleExtendSession = useCallback(async () => {
    try {
      console.log('🔄 세션 연장 시도...');

      // 간단한 API 호출로 토큰 활성화 (userAPI.me()는 인증이 필요한 간단한 엔드포인트)
      await userAPI.me();

      console.log('✅ 세션 연장 완료 (API 호출 성공)');
      setShowWarning(false);
      setTimeRemaining(null);

      // 성공 메시지
      alert('세션이 연장되었습니다. 계속 사용하실 수 있습니다.');
    } catch (error) {
      console.error('❌ 세션 연장 실패:', error);

      // 토큰이 이미 만료되었거나 API 호출 실패
      alert('세션을 연장할 수 없습니다. 다시 로그인해주세요.');
      handleLogout();
    }
  }, []);

  // 로그아웃 처리
  const handleLogout = useCallback(() => {
    console.log('🚪 자동 로그아웃 (토큰 만료)');

    // 현재 경로 저장 (로그인 후 돌아오기 위해)
    const currentPath = window.location.pathname + window.location.search;
    if (currentPath !== '/login' && currentPath !== '/signup') {
      sessionStorage.setItem('redirectAfterLogin', currentPath);
    }

    // 토큰 제거
    localStorage.removeItem('accessToken');

    // 커스텀 이벤트 발송
    window.dispatchEvent(new Event('auth-logout'));

    // 경고 모달 닫기
    setShowWarning(false);

    // 로그인 페이지로 이동
    navigate('/login', {
      state: { message: '세션이 만료되었습니다. 다시 로그인해주세요.' }
    });
  }, [navigate]);

  // 주기적으로 토큰 체크 (1분마다)
  useEffect(() => {
    // 토큰이 없으면 아무것도 하지 않음
    const token = localStorage.getItem('accessToken');
    if (!token) {
      return;
    }

    const interval = setInterval(() => {
      const result = checkTokenExpiry();

      switch (result.status) {
        case 'none':
        case 'invalid':
          // 토큰 없음 or 유효하지 않음 - 아무것도 안 함
          break;

        case 'expired':
          // 토큰 만료됨 - 자동 로그아웃
          console.log('⏰ 토큰 만료 감지 - 자동 로그아웃');
          handleLogout();
          break;

        case 'expiring':
          // 토큰 만료 임박 - 경고 표시
          console.log('⚠️  토큰 만료 임박:', result.timeLeft, '초 남음');
          setShowWarning(true);
          setTimeRemaining(result.timeLeft);
          break;

        case 'valid':
          // 토큰 유효 - 경고 숨김
          setShowWarning(false);
          break;

        default:
          break;
      }
    }, 60000); // 1분마다 체크

    // 컴포넌트 마운트 시 즉시 한 번 체크
    const initialResult = checkTokenExpiry();
    if (initialResult.status === 'expiring') {
      setShowWarning(true);
      setTimeRemaining(initialResult.timeLeft);
    } else if (initialResult.status === 'expired') {
      handleLogout();
    }

    return () => clearInterval(interval);
  }, [checkTokenExpiry, handleLogout]);

  // timeRemaining을 MM:SS 형식으로 변환
  const formatTime = (seconds) => {
    if (!seconds) return '--:--';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  if (!showWarning) {
    return null;
  }

  return (
    <div className="token-expiry-overlay">
      <div className="token-expiry-modal">
        <div className="modal-icon">⏰</div>
        <h2>세션 만료 예정</h2>
        <p>
          로그인 세션이 곧 만료됩니다.
          <br />
          계속 사용하시려면 아래 버튼을 클릭해주세요.
        </p>
        <div className="time-remaining">
          남은 시간: <strong>{formatTime(timeRemaining)}</strong>
        </div>
        <div className="modal-actions">
          <button
            className="btn-extend"
            onClick={handleExtendSession}
          >
            계속 사용하기
          </button>
          <button
            className="btn-logout"
            onClick={handleLogout}
          >
            로그아웃
          </button>
        </div>
      </div>
    </div>
  );
}

export default TokenExpiryWarning;
