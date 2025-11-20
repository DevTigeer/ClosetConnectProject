import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { marketChatAPI } from '../services/api';
import { getProductImageUrl, handleImageError } from '../utils/imageUtils';
import './MarketChatListPage.css';

function MarketChatListPage() {
  const navigate = useNavigate();
  const token = localStorage.getItem('accessToken');
  const [chatRooms, setChatRooms] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }
    fetchChatRooms();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // 컴포넌트 마운트 시 한 번만 실행

  const fetchChatRooms = async () => {
    try {
      const response = await marketChatAPI.listRooms();
      setChatRooms(response.data || []);
    } catch (error) {
      console.error('채팅방 목록 조회 실패:', error);
      setChatRooms([]);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now - date;
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return '방금 전';
    if (minutes < 60) return `${minutes}분 전`;
    if (hours < 24) return `${hours}시간 전`;
    if (days < 7) return `${days}일 전`;
    return date.toLocaleDateString();
  };

  return (
    <div className="market-chat-list-page">
      <div className="page-container">
        <header className="page-header">
          <h1 className="page-title">채팅</h1>
          <p className="page-subtitle">중고거래 채팅 목록</p>
        </header>

        {loading ? (
          <div className="loading-state">
            <div className="spinner"></div>
            <p>로딩 중...</p>
          </div>
        ) : (
          <>
            {chatRooms.length > 0 ? (
              <div className="chat-rooms-list">
                {chatRooms.map((room) => (
                  <div
                    key={room.roomId}
                    className="chat-room-item"
                    onClick={() => navigate(`/market/chat/${room.roomId}`)}
                  >
                    <div className="chat-room-product-image">
                      <img
                        src={room.productImageUrl || '/placeholder.jpg'}
                        alt={room.productTitle}
                        onError={handleImageError}
                      />
                    </div>
                    <div className="chat-room-info">
                      <div className="chat-room-header">
                        <h3 className="chat-room-product-title">
                          {room.productTitle}
                        </h3>
                        {room.lastMessageTime && (
                          <span className="chat-room-time">
                            {formatDate(room.lastMessageTime)}
                          </span>
                        )}
                      </div>
                      <div className="chat-room-partner">
                        {room.partnerNickname}
                      </div>
                      {room.lastMessage && (
                        <div className="chat-room-last-message">
                          {room.lastMessage}
                        </div>
                      )}
                    </div>
                    {room.unreadCount > 0 && (
                      <div className="chat-room-unread-badge">
                        {room.unreadCount}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="empty-state">
                <div className="empty-icon">💬</div>
                <h3 className="empty-title">채팅 내역이 없습니다</h3>
                <p className="empty-text">
                  중고거래 상품에서 채팅하기를 눌러 대화를 시작하세요
                </p>
                <button
                  className="btn-primary"
                  onClick={() => navigate('/market')}
                >
                  중고거래 보러가기
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default MarketChatListPage;
