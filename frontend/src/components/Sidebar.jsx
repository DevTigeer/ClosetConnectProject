import { useState, useEffect } from 'react';
import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { userAPI, boardAPI } from '../services/api';
import './Sidebar.css';

function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const token = localStorage.getItem('accessToken');
  const [isAdmin, setIsAdmin] = useState(false);
  const [userInfo, setUserInfo] = useState(null);
  const [boards, setBoards] = useState([]);

  useEffect(() => {
    if (token) {
      fetchUserInfo();
    }
    fetchBoards();
  }, [token]);

  const fetchUserInfo = async () => {
    try {
      const response = await userAPI.me();
      setUserInfo(response.data);
      setIsAdmin(response.data.role === 'ADMIN' || response.data.role === 'ROLE_ADMIN');
    } catch (err) {
      console.error(err);
    }
  };

  const fetchBoards = async () => {
    try {
      const response = await boardAPI.listPublic();
      setBoards(response.data || []);
    } catch (err) {
      console.error('게시판 목록 조회 실패:', err);
    }
  };

  // 현재 페이지가 중고거래 관련인지 확인
  const isMarketSection = location.pathname.startsWith('/market');
  // 현재 페이지가 커뮤니티 관련인지 확인
  const isCommunitySection = location.pathname.startsWith('/community');

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    navigate('/');
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-top">
        <div className="logo" onClick={() => navigate('/')}>
          <h2>ClosetConnect</h2>
          <p className="logo-subtitle">패션 라이프 플랫폼</p>
        </div>

        {token && userInfo && (
          <div className="user-profile">
            <div className="user-avatar">
              {userInfo.nickname?.charAt(0) || 'U'}
            </div>
            <div className="user-info">
              <div className="user-name">{userInfo.nickname}</div>
              <div className="user-email">{userInfo.email}</div>
            </div>
          </div>
        )}

        <nav className="nav-menu">
          {/* 중고거래 섹션에 있을 때 */}
          {isMarketSection ? (
            <>
              <div className="nav-section">
                <div className="nav-section-title">중고거래</div>
                <NavLink to="/market" className="nav-item">
                  <span className="nav-icon">🛍️</span>
                  <span className="nav-text">상품 목록</span>
                </NavLink>

                {token && (
                  <>
                    <NavLink to="/market/chat" className="nav-item">
                      <span className="nav-icon">💭</span>
                      <span className="nav-text">채팅</span>
                    </NavLink>

                    <NavLink to="/market/orders" className="nav-item">
                      <span className="nav-icon">📦</span>
                      <span className="nav-text">주문 내역</span>
                    </NavLink>

                    <NavLink to="/market/likes" className="nav-item">
                      <span className="nav-icon">💝</span>
                      <span className="nav-text">찜 목록</span>
                    </NavLink>
                  </>
                )}
              </div>

              <div className="nav-divider"></div>

              <div className="nav-section">
                <div className="nav-section-title">다른 메뉴</div>
                <NavLink to="/closet" className="nav-item">
                  <span className="nav-icon">👔</span>
                  <span className="nav-text">내 옷장</span>
                </NavLink>

                <NavLink to="/recommend" className="nav-item">
                  <span className="nav-icon">🌤️</span>
                  <span className="nav-text">날씨 추천</span>
                </NavLink>

                <NavLink to="/community" className="nav-item">
                  <span className="nav-icon">💬</span>
                  <span className="nav-text">커뮤니티</span>
                </NavLink>
              </div>
            </>
          ) : isCommunitySection ? (
            /* 커뮤니티 섹션에 있을 때 */
            <>
              <div className="nav-section">
                <div className="nav-section-title">게시판</div>
                {boards.map((board) => (
                  <NavLink
                    key={board.id}
                    to={`/community/${board.slug}`}
                    className="nav-item"
                  >
                    <span className="nav-icon">📋</span>
                    <span className="nav-text">{board.name}</span>
                  </NavLink>
                ))}
              </div>

              <div className="nav-divider"></div>

              <div className="nav-section">
                <div className="nav-section-title">다른 메뉴</div>
                <NavLink to="/closet" className="nav-item">
                  <span className="nav-icon">👔</span>
                  <span className="nav-text">내 옷장</span>
                </NavLink>

                <NavLink to="/recommend" className="nav-item">
                  <span className="nav-icon">🌤️</span>
                  <span className="nav-text">날씨 추천</span>
                </NavLink>

                <NavLink to="/market" className="nav-item">
                  <span className="nav-icon">🛍️</span>
                  <span className="nav-text">중고거래</span>
                </NavLink>
              </div>
            </>
          ) : (
            /* 기본 메뉴 (옷장, 추천 등) */
            <>
              <div className="nav-section">
                <div className="nav-section-title">메인 메뉴</div>
                <NavLink to="/closet" className="nav-item">
                  <span className="nav-icon">👔</span>
                  <span className="nav-text">내 옷장</span>
                </NavLink>

                <NavLink to="/recommend" className="nav-item">
                  <span className="nav-icon">🌤️</span>
                  <span className="nav-text">날씨 추천</span>
                </NavLink>

                <NavLink to="/market" className="nav-item">
                  <span className="nav-icon">🛍️</span>
                  <span className="nav-text">중고거래</span>
                </NavLink>

                <NavLink to="/community" className="nav-item">
                  <span className="nav-icon">💬</span>
                  <span className="nav-text">커뮤니티</span>
                </NavLink>
              </div>
            </>
          )}

          <div className="nav-divider"></div>

          <div className="nav-section">
            <div className="nav-section-title">내 정보</div>
            {token ? (
              <NavLink to="/mypage" className="nav-item">
                <span className="nav-icon">👤</span>
                <span className="nav-text">마이페이지</span>
              </NavLink>
            ) : (
              <>
                <NavLink to="/login" className="nav-item">
                  <span className="nav-icon">🔐</span>
                  <span className="nav-text">로그인</span>
                </NavLink>
                <NavLink to="/signup" className="nav-item">
                  <span className="nav-icon">✨</span>
                  <span className="nav-text">회원가입</span>
                </NavLink>
              </>
            )}
          </div>
        </nav>
      </div>

      {token && (
        <div className="sidebar-bottom">
          {isAdmin && (
            <NavLink to="/admin" className="admin-btn">
              <span>⚙️</span>
              <span>관리자 페이지</span>
            </NavLink>
          )}
          <button onClick={handleLogout} className="logout-btn">
            <span>🚪</span>
            <span>로그아웃</span>
          </button>
        </div>
      )}
    </aside>
  );
}

export default Sidebar;
