import { useState, useEffect } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { authAPI } from '../services/api';
import './AuthPage.css';

function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionExpiredMessage, setSessionExpiredMessage] = useState('');

  // URL 파라미터에서 세션 만료 메시지 확인
  useEffect(() => {
    if (searchParams.get('session') === 'expired') {
      setSessionExpiredMessage('세션이 만료되었습니다. 다시 로그인해주세요.');
    }
  }, [searchParams]);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await authAPI.login(formData);
      const token = response.data.accessToken;

      localStorage.setItem('accessToken', token);

      // 로그인 후 복귀 경로 확인
      const redirectPath = sessionStorage.getItem('redirectAfterLogin');
      if (redirectPath) {
        console.log('✅ 로그인 성공 - 이전 페이지로 복귀:', redirectPath);
        sessionStorage.removeItem('redirectAfterLogin');
        navigate(redirectPath);
      } else {
        console.log('✅ 로그인 성공 - 기본 페이지로 이동');
        navigate('/closet');
      }
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || '로그인에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-wrap">
        <h1 className="auth-title">로그인</h1>

        {sessionExpiredMessage && (
          <div className="auth-warning">{sessionExpiredMessage}</div>
        )}

        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="auth-field">
            <label htmlFor="email">이메일</label>
            <input
              id="email"
              name="email"
              type="email"
              placeholder="you@example.com"
              value={formData.email}
              onChange={handleChange}
              required
            />
          </div>

          <div className="auth-field">
            <label htmlFor="password">비밀번호</label>
            <input
              id="password"
              name="password"
              type="password"
              placeholder="********"
              value={formData.password}
              onChange={handleChange}
              required
            />
          </div>

          <button type="submit" className="auth-submit" disabled={loading}>
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>

        <div className="auth-helper">
          <Link to="/signup">아직 계정이 없나요?</Link>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
