import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { marketChatAPI } from '../services/api';
import './MarketChatRoomPage.css';

function MarketChatRoomPage() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  const token = localStorage.getItem('accessToken');
  const messagesEndRef = useRef(null);

  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }
    fetchMessages();
    // 읽음 처리
    markAsRead();

    // 주기적으로 메시지 갱신 (폴링 방식)
    const interval = setInterval(fetchMessages, 3000);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId]); // roomId가 변경될 때만 실행

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const fetchMessages = async () => {
    try {
      const response = await marketChatAPI.listMessages(roomId);
      setMessages(response.data || []);
    } catch (error) {
      console.error('메시지 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const markAsRead = async () => {
    try {
      await marketChatAPI.markAsRead(roomId);
    } catch (error) {
      console.error('읽음 처리 실패:', error);
    }
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!newMessage.trim() || sending) return;

    setSending(true);
    try {
      await marketChatAPI.sendMessage(roomId, {
        content: newMessage,
        messageType: 'TEXT'
      });
      setNewMessage('');
      await fetchMessages();
      scrollToBottom();
    } catch (error) {
      console.error('메시지 전송 실패:', error);
      alert('메시지 전송에 실패했습니다.');
    } finally {
      setSending(false);
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const formatTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    });
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    if (date.toDateString() === today.toDateString()) {
      return '오늘';
    } else if (date.toDateString() === yesterday.toDateString()) {
      return '어제';
    } else {
      return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    }
  };

  const groupMessagesByDate = (messages) => {
    const groups = {};
    messages.forEach((msg) => {
      const date = new Date(msg.createdAt).toDateString();
      if (!groups[date]) {
        groups[date] = [];
      }
      groups[date].push(msg);
    });
    return groups;
  };

  const messageGroups = groupMessagesByDate(messages);

  if (loading) {
    return (
      <div className="market-chat-room-page">
        <div className="page-container">
          <div className="loading-state">
            <div className="spinner"></div>
            <p>로딩 중...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="market-chat-room-page">
      <div className="chat-room-container">
        <header className="chat-room-header">
          <button onClick={() => navigate('/market/chat')} className="back-btn">
            ← 뒤로
          </button>
          <h2 className="chat-room-title">채팅</h2>
        </header>

        <div className="chat-messages-container">
          {Object.keys(messageGroups).map((dateKey) => (
            <div key={dateKey}>
              <div className="date-divider">
                <span>{formatDate(messageGroups[dateKey][0].createdAt)}</span>
              </div>
              {messageGroups[dateKey].map((msg) => (
                <div
                  key={msg.messageId}
                  className={`chat-message ${msg.isMine ? 'mine' : 'other'} ${
                    msg.messageType === 'SYSTEM' ? 'system' : ''
                  }`}
                >
                  {msg.messageType === 'SYSTEM' ? (
                    <div className="system-message">{msg.content}</div>
                  ) : (
                    <>
                      {!msg.isMine && (
                        <div className="message-sender">{msg.senderNickname}</div>
                      )}
                      <div className="message-bubble">
                        <div className="message-content">{msg.content}</div>
                        <div className="message-time">{formatTime(msg.createdAt)}</div>
                      </div>
                    </>
                  )}
                </div>
              ))}
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>

        <form onSubmit={handleSendMessage} className="chat-input-form">
          <input
            type="text"
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            placeholder="메시지를 입력하세요"
            className="chat-input"
            disabled={sending}
          />
          <button
            type="submit"
            className="send-btn"
            disabled={!newMessage.trim() || sending}
          >
            {sending ? '전송 중...' : '전송'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default MarketChatRoomPage;
