import { useState, useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { userAPI } from '../services/api';
import './Sidebar.css';

function Sidebar() {
  const navigate = useNavigate();
  const token = localStorage.getItem('accessToken');
  const [isAdmin, setIsAdmin] = useState(false);

  useEffect(() => {
    if (token) {
      fetchUserInfo();
    }
  }, [token]);

  const fetchUserInfo = async () => {
    try {
      const response = await userAPI.me();
      // role이 ADMIN이면 관리자
      setIsAdmin(response.data.role === 'ADMIN');
    } catch (err) {
      console.error(err);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    navigate('/login');
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-top">
        <div className="logo">
          <h2>ClosetConnect</h2>
        </div>

        <nav className="nav-menu">
          {!token ? (
            <NavLink to="/login" className="nav-item">
              로그인
            </NavLink>
          ) : null}

          {token && (
            <>
              <NavLink to="/closet" className="nav-item">
                옷장
              </NavLink>

              <NavLink to="/community" className="nav-item">
                커뮤니티
              </NavLink>

              <NavLink to="/mypage" className="nav-item">
                마이페이지
              </NavLink>
            </>
          )}
        </nav>
      </div>

      {token && (
        <div className="sidebar-bottom">
          {isAdmin && (
            <NavLink to="/admin" className="admin-btn">
              ⚙️ 관리자
            </NavLink>
          )}
          <button onClick={handleLogout} className="logout-btn">
            로그아웃
          </button>
        </div>
      )}
    </aside>
  );
}

export default Sidebar;
