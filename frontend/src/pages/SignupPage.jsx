import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authAPI } from '../services/api';
import './AuthPage.css';

function SignupPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    nickname: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

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
      await authAPI.signup(formData);
      alert('회원가입이 완료되었습니다! 로그인해주세요.');
      navigate('/login');
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || '회원가입에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-wrap">
        <h1 className="auth-title">회원가입</h1>

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
            <label htmlFor="nickname">닉네임</label>
            <input
              id="nickname"
              name="nickname"
              type="text"
              placeholder="닉네임"
              value={formData.nickname}
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
            {loading ? '가입 중...' : '회원가입'}
          </button>
        </form>

        <div className="auth-helper">
          <Link to="/login">이미 계정이 있나요?</Link>
        </div>
      </div>
    </div>
  );
}

export default SignupPage;
