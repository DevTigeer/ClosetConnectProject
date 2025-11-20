import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { marketOrderAPI } from '../services/api';
import { getImageUrl, handleImageError } from '../utils/imageUtils';
import './MyOrdersPage.css';

function MyOrdersPage() {
  const navigate = useNavigate();
  const token = localStorage.getItem('accessToken');
  const [activeTab, setActiveTab] = useState('buyer'); // 'buyer' or 'seller'
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  useEffect(() => {
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }
    fetchOrders(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab]); // activeTab이 변경될 때만 실행

  const fetchOrders = async (pageNum) => {
    setLoading(true);
    try {
      const params = { page: pageNum, size: 20 };
      const response = activeTab === 'buyer'
        ? await marketOrderAPI.getBuyerOrders(params)
        : await marketOrderAPI.getSellerOrders(params);

      if (pageNum === 0) {
        setOrders(response.data.content || []);
      } else {
        setOrders(prev => [...prev, ...(response.data.content || [])]);
      }

      setHasMore(!response.data.last);
      setPage(pageNum);
    } catch (error) {
      console.error('주문 내역 조회 실패:', error);
      setOrders([]);
    } finally {
      setLoading(false);
    }
  };

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setPage(0);
    setOrders([]);
  };

  const getStatusBadge = (statusName) => {
    // orderStatusName을 직접 사용 (한글로 이미 변환됨)
    const statusClassMap = {
      '결제대기': 'pending',
      '결제완료': 'paid',
      '발송완료': 'shipping',
      '배송완료': 'delivered',
      '구매확정': 'confirmed',
      '주문취소': 'canceled',
      '환불완료': 'refunded',
    };

    const className = statusClassMap[statusName] || 'default';
    return <span className={`status-badge ${className}`}>{statusName}</span>;
  };

  const formatPrice = (price) => {
    return price?.toLocaleString() + '원' || '0원';
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  return (
    <div className="my-orders-page">
      <div className="page-container">
        <header className="page-header">
          <h1 className="page-title">주문 내역</h1>
          <p className="page-subtitle">구매/판매 내역을 확인하세요</p>
        </header>

        <div className="orders-tabs">
          <button
            className={`tab-btn ${activeTab === 'buyer' ? 'active' : ''}`}
            onClick={() => handleTabChange('buyer')}
          >
            구매 내역
          </button>
          <button
            className={`tab-btn ${activeTab === 'seller' ? 'active' : ''}`}
            onClick={() => handleTabChange('seller')}
          >
            판매 내역
          </button>
        </div>

        {loading && page === 0 ? (
          <div className="loading-state">
            <div className="spinner"></div>
            <p>로딩 중...</p>
          </div>
        ) : (
          <>
            {orders.length > 0 ? (
              <div className="orders-list">
                {orders.map((order) => (
                  <div
                    key={order.orderId}
                    className="order-card"
                    onClick={() => navigate(`/market/orders/${order.orderId}`)}
                  >
                    <div className="order-header">
                      <div className="order-date">
                        {formatDate(order.createdAt)}
                      </div>
                      {getStatusBadge(order.orderStatusName)}
                    </div>

                    <div className="order-content">
                      <div className="product-title-box">
                        <h3 className="product-title">{order.productTitle}</h3>
                      </div>

                      <div className="product-image-section">
                        <img
                          src={getImageUrl(order.productThumbnail)}
                          alt={order.productTitle}
                          onError={handleImageError}
                          className="product-thumbnail"
                        />
                      </div>

                      <div className="product-meta">
                        {activeTab === 'buyer' ? (
                          <span>{order.sellerNickname}</span>
                        ) : (
                          <span>{order.buyerNickname}</span>
                        )}
                      </div>

                      <div className="order-price-box">
                        <span className="price-icon">💰</span>
                        <span className="order-price">{formatPrice(order.orderAmount)}</span>
                      </div>
                    </div>

                    <div className="order-footer">
                      <div className="order-id">주문번호: {order.tossOrderId}</div>
                      {order.trackingNumber && (
                        <div className="tracking-info">
                          운송장: {order.shippingCompany} {order.trackingNumber}
                        </div>
                      )}
                    </div>
                  </div>
                ))}

                {hasMore && (
                  <button
                    className="load-more-btn"
                    onClick={() => fetchOrders(page + 1)}
                    disabled={loading}
                  >
                    {loading ? '로딩 중...' : '더 보기'}
                  </button>
                )}
              </div>
            ) : (
              <div className="empty-state">
                <div className="empty-icon">📦</div>
                <h3 className="empty-title">
                  {activeTab === 'buyer' ? '구매 내역이 없습니다' : '판매 내역이 없습니다'}
                </h3>
                <p className="empty-text">
                  {activeTab === 'buyer'
                    ? '중고거래에서 마음에 드는 상품을 구매해보세요'
                    : '옷장의 아이템을 중고거래에 등록해보세요'}
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

export default MyOrdersPage;
