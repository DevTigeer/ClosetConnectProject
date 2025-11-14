import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import ClosetPage from './pages/ClosetPage';
import CommunityPage from './pages/CommunityPage';
import BoardPage from './pages/BoardPage';
import PostDetailPage from './pages/PostDetailPage';
import CreatePostPage from './pages/CreatePostPage';
import MyPage from './pages/MyPage';
import './App.css';

// PrivateRoute: 인증이 필요한 페이지
function PrivateRoute({ children }) {
  const token = localStorage.getItem('accessToken');
  return token ? children : <Navigate to="/login" replace />;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 인증 페이지 */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />

        {/* 메인 레이아웃 (사이드바 포함) */}
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/closet" replace />} />

          {/* 옷장 (인증 필요) */}
          <Route
            path="closet"
            element={
              <PrivateRoute>
                <ClosetPage />
              </PrivateRoute>
            }
          />

          {/* 커뮤니티 */}
          <Route path="community" element={<CommunityPage />} />
          <Route path="community/:slug" element={<BoardPage />} />
          <Route path="community/:slug/posts/:postId" element={<PostDetailPage />} />
          <Route
            path="community/:slug/new"
            element={
              <PrivateRoute>
                <CreatePostPage />
              </PrivateRoute>
            }
          />

          {/* 마이페이지 (인증 필요) */}
          <Route
            path="mypage"
            element={
              <PrivateRoute>
                <MyPage />
              </PrivateRoute>
            }
          />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
