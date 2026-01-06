import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ClothUploadProvider } from './contexts/ClothUploadContext';
import Layout from './components/Layout';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import ClosetPage from './pages/ClosetPage';
import OOTDPage from './pages/OOTDPage';
import RecommendPage from './pages/RecommendPage';
import MarketPage from './pages/MarketPage';
import MarketDetailPage from './pages/MarketDetailPage';
import MarketCreatePage from './pages/MarketCreatePage';
import MarketChatListPage from './pages/MarketChatListPage';
import MarketChatRoomPage from './pages/MarketChatRoomPage';
import MyOrdersPage from './pages/MyOrdersPage';
import OrderDetailPage from './pages/OrderDetailPage';
import MyLikesPage from './pages/MyLikesPage';
import CommunityPage from './pages/CommunityPage';
import BoardPage from './pages/BoardPage';
import PostDetailPage from './pages/PostDetailPage';
import CreatePostPage from './pages/CreatePostPage';
import MyPage from './pages/MyPage';
import AdminPage from './pages/AdminPage';
import './App.css';

// PrivateRoute: 인증이 필요한 페이지
function PrivateRoute({ children }) {
  const token = localStorage.getItem('accessToken');
  return token ? children : <Navigate to="/login" replace />;
}

function App() {
  return (
    <ClothUploadProvider>
      <BrowserRouter>
        <Routes>
        {/* 랜딩 페이지 (레이아웃 없음) */}
        <Route path="/home" element={<LandingPage />} />

        {/* 인증 페이지 (레이아웃 없음) */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />

        {/* 메인 레이아웃 (사이드바 포함) */}
        <Route path="/" element={<Layout />}>
          {/* 메인 페이지: 옷장 */}
          <Route index element={<Navigate to="/closet" replace />} />

          {/* 옷장 (메인 페이지, 비로그인시 유도 UI 표시) */}
          <Route path="closet" element={<ClosetPage />} />

          {/* OOTD (인증 필요) */}
          <Route
            path="ootd"
            element={
              <PrivateRoute>
                <OOTDPage />
              </PrivateRoute>
            }
          />

          {/* 추천 (인증 필요) */}
          <Route
            path="recommend"
            element={
              <PrivateRoute>
                <RecommendPage />
              </PrivateRoute>
            }
          />

          {/* 중고거래 */}
          <Route path="market" element={<MarketPage />} />
          <Route path="market/products/:id" element={<MarketDetailPage />} />
          <Route
            path="market/create"
            element={
              <PrivateRoute>
                <MarketCreatePage />
              </PrivateRoute>
            }
          />

          {/* 중고거래 - 채팅 */}
          <Route
            path="market/chat"
            element={
              <PrivateRoute>
                <MarketChatListPage />
              </PrivateRoute>
            }
          />
          <Route
            path="market/chat/:roomId"
            element={
              <PrivateRoute>
                <MarketChatRoomPage />
              </PrivateRoute>
            }
          />

          {/* 중고거래 - 주문 내역 */}
          <Route
            path="market/orders"
            element={
              <PrivateRoute>
                <MyOrdersPage />
              </PrivateRoute>
            }
          />
          <Route
            path="market/orders/:orderId"
            element={
              <PrivateRoute>
                <OrderDetailPage />
              </PrivateRoute>
            }
          />

          {/* 중고거래 - 찜 목록 */}
          <Route
            path="market/likes"
            element={
              <PrivateRoute>
                <MyLikesPage />
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

          {/* 관리자 페이지 (인증 필요) */}
          <Route
            path="admin"
            element={
              <PrivateRoute>
                <AdminPage />
              </PrivateRoute>
            }
          />
        </Route>
        </Routes>
      </BrowserRouter>
    </ClothUploadProvider>
  );
}

export default App;
