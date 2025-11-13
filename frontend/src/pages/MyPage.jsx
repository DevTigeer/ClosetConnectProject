import { useState, useEffect } from 'react';
import { userAPI } from '../services/api';
import './MyPage.css';

function MyPage() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchUser = async () => {
      setLoading(true);
      try {
        const response = await userAPI.me();
        setUser(response.data);
      } catch (err) {
        console.error(err);
        alert('사용자 정보를 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchUser();
  }, []);

  return (
    <div className="mypage">
      <header className="mypage-header">
        <h1>마이페이지</h1>
      </header>

      {loading ? (
        <div className="loading">로딩 중...</div>
      ) : user ? (
        <div className="mypage-content">
          <div className="user-card">
            <div className="user-avatar">
              {user.nickname?.[0]?.toUpperCase() || 'U'}
            </div>

            <div className="user-info">
              <div className="info-row">
                <span className="info-label">닉네임</span>
                <span className="info-value">{user.nickname || '-'}</span>
              </div>
              <div className="info-row">
                <span className="info-label">이메일</span>
                <span className="info-value">{user.email || '-'}</span>
              </div>
              <div className="info-row">
                <span className="info-label">가입일</span>
                <span className="info-value">
                  {user.createdAt ? user.createdAt.split('T')[0] : '-'}
                </span>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="empty-state">
          <p>사용자 정보를 불러올 수 없습니다.</p>
        </div>
      )}
    </div>
  );
}

export default MyPage;
