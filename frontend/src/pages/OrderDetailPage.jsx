import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { marketOrderAPI } from '../services/api';
import { getImageUrl, handleImageError } from '../utils/imageUtils';
import './OrderDetailPage.css';

function OrderDetailPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const token = localStorage.getItem('accessToken');
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);

  // 배송 처리용 상태
  const [showShipModal, setShowShipModal] = useState(false);
  const [shippingCompany, setShippingCompany] = useState('');
  const [trackingNumber, setTrackingNumber] = useState('');

  useEffect(() => {
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }
    fetchOrderDetail();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderId]);

  const fetchOrderDetail = async () => {
    setLoading(true);
    try {
      const response = await marketOrderAPI.getOne(orderId);
      setOrder(response.data);
    } catch (error) {
      console.error('주문 조회 실패:', error);
      alert('주문을 찾을 수 없습니다.');
      navigate('/market/orders');
    } finally {
      setLoading(false);
    }
  };

  const handleShip = async (e) => {
    e.preventDefault();
    if (!shippingCompany || !trackingNumber) {
      alert('배송사와 운송장 번호를 입력해주세요.');
      return;
    }

    setIsProcessing(true);
    try {
      await marketOrderAPI.ship(orderId, {
        shippingCompany,
        trackingNumber,
      });
      alert('발송 처리가 완료되었습니다.');
      setShowShipModal(false);
      fetchOrderDetail();
    } catch (error) {
      console.error('발송 처리 실패:', error);
      alert(error.response?.data?.message || '발송 처리에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleConfirm = async () => {
    if (!window.confirm('구매를 확정하시겠습니까? 확정 후에는 취소할 수 없습니다.')) {
      return;
    }

    setIsProcessing(true);
    try {
      await marketOrderAPI.confirm(orderId);
      alert('구매 확정이 완료되었습니다.');
      fetchOrderDetail();
    } catch (error) {
      console.error('구매 확정 실패:', error);
      alert(error.response?.data?.message || '구매 확정에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleCancel = async () => {
    const reason = window.prompt('취소 사유를 입력해주세요:');
    if (!reason) return;

    setIsProcessing(true);
    try {
      await marketOrderAPI.cancel(orderId, { cancelReason: reason });
      alert('주문이 취소되었습니다.');
      fetchOrderDetail();
    } catch (error) {
      console.error('주문 취소 실패:', error);
      alert(error.response?.data?.message || '주문 취소에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  const getStatusBadge = (statusName) => {
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

  const formatDateTime = (dateString) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getUserId = () => {
    try {
      const token = localStorage.getItem('accessToken');
      if (!token) return null;
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.userId;
    } catch {
      return null;
    }
  };

  const currentUserId = getUserId();
  const isBuyer = order?.buyerId === currentUserId;
  const isSeller = order?.sellerId === currentUserId;

  if (loading) {
    return (
      <div className="order-detail-page">
        <div className="loading-state">
          <div className="spinner"></div>
          <p>로딩 중...</p>
        </div>
      </div>
    );
  }

  if (!order) {
    return null;
  }

  return (
    <div className="order-detail-page">
      <div className="page-container">
        <header className="page-header">
          <button className="back-btn" onClick={() => navigate('/market/orders')}>
            ← 목록으로
          </button>
          <h1 className="page-title">주문 상세</h1>
        </header>

        <div className="order-status-card">
          <div className="status-header">
            <h2>주문 상태</h2>
            {getStatusBadge(order.orderStatusName)}
          </div>
          <div className="order-id-info">
            주문번호: {order.tossOrderId}
          </div>
        </div>

        <div className="order-product-card">
          <h2>상품 정보</h2>
          <div className="product-info-row">
            {order.productThumbnail && (
              <div className="product-thumbnail">
                <img
                  src={getImageUrl(order.productThumbnail)}
                  alt={order.productTitle}
                  onError={handleImageError}
                />
              </div>
            )}
            <div className="product-details">
              <h3>{order.productTitle}</h3>
              <p className="product-price">{formatPrice(order.orderAmount)}</p>
            </div>
          </div>
        </div>

        <div className="order-parties-card">
          <h2>거래 정보</h2>
          <div className="info-row">
            <span className="label">판매자</span>
            <span className="value">{order.sellerNickname}</span>
          </div>
          <div className="info-row">
            <span className="label">구매자</span>
            <span className="value">{order.buyerNickname}</span>
          </div>
        </div>

        <div className="order-payment-card">
          <h2>결제 정보</h2>
          <div className="info-row">
            <span className="label">결제 금액</span>
            <span className="value">{formatPrice(order.orderAmount)}</span>
          </div>
          {order.paymentMethodName && (
            <div className="info-row">
              <span className="label">결제 수단</span>
              <span className="value">{order.paymentMethodName}</span>
            </div>
          )}
          {order.approvedAt && (
            <div className="info-row">
              <span className="label">결제 완료 시간</span>
              <span className="value">{formatDateTime(order.approvedAt)}</span>
            </div>
          )}
        </div>

        {(order.shippingCompany || order.trackingNumber) && (
          <div className="order-shipping-card">
            <h2>배송 정보</h2>
            {order.shippingCompany && (
              <div className="info-row">
                <span className="label">배송사</span>
                <span className="value">{order.shippingCompany}</span>
              </div>
            )}
            {order.trackingNumber && (
              <div className="info-row">
                <span className="label">운송장 번호</span>
                <span className="value">{order.trackingNumber}</span>
              </div>
            )}
            {order.shippedAt && (
              <div className="info-row">
                <span className="label">발송 시간</span>
                <span className="value">{formatDateTime(order.shippedAt)}</span>
              </div>
            )}
          </div>
        )}

        <div className="order-timeline-card">
          <h2>주문 이력</h2>
          <div className="timeline">
            <div className="timeline-item">
              <span className="label">주문 생성</span>
              <span className="value">{formatDateTime(order.createdAt)}</span>
            </div>
            {order.approvedAt && (
              <div className="timeline-item">
                <span className="label">결제 완료</span>
                <span className="value">{formatDateTime(order.approvedAt)}</span>
              </div>
            )}
            {order.shippedAt && (
              <div className="timeline-item">
                <span className="label">발송 완료</span>
                <span className="value">{formatDateTime(order.shippedAt)}</span>
              </div>
            )}
            {order.confirmedAt && (
              <div className="timeline-item">
                <span className="label">구매 확정</span>
                <span className="value">{formatDateTime(order.confirmedAt)}</span>
              </div>
            )}
            {order.settledAt && (
              <div className="timeline-item">
                <span className="label">정산 완료</span>
                <span className="value">{formatDateTime(order.settledAt)}</span>
              </div>
            )}
            {order.refundedAt && (
              <div className="timeline-item">
                <span className="label">환불 완료</span>
                <span className="value">{formatDateTime(order.refundedAt)}</span>
              </div>
            )}
          </div>
        </div>

        {order.cancelReason && (
          <div className="order-cancel-card">
            <h2>취소 사유</h2>
            <p>{order.cancelReason}</p>
          </div>
        )}

        <div className="order-actions">
          {isSeller && order.orderStatusName === '결제완료' && (
            <button
              className="btn-primary"
              onClick={() => setShowShipModal(true)}
              disabled={isProcessing}
            >
              발송 처리
            </button>
          )}
          {isBuyer && order.orderStatusName === '발송완료' && (
            <button
              className="btn-primary"
              onClick={handleConfirm}
              disabled={isProcessing}
            >
              구매 확정
            </button>
          )}
          {order.orderStatusName === '결제대기' && (
            <button
              className="btn-danger"
              onClick={handleCancel}
              disabled={isProcessing}
            >
              주문 취소
            </button>
          )}
        </div>
      </div>

      {/* 발송 처리 모달 */}
      {showShipModal && (
        <div className="modal-overlay" onClick={() => setShowShipModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>발송 처리</h2>
            <form onSubmit={handleShip}>
              <div className="form-group">
                <label>배송사</label>
                <input
                  type="text"
                  value={shippingCompany}
                  onChange={(e) => setShippingCompany(e.target.value)}
                  placeholder="예: CJ대한통운, 우체국"
                  required
                />
              </div>
              <div className="form-group">
                <label>운송장 번호</label>
                <input
                  type="text"
                  value={trackingNumber}
                  onChange={(e) => setTrackingNumber(e.target.value)}
                  placeholder="운송장 번호를 입력하세요"
                  required
                />
              </div>
              <div className="modal-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setShowShipModal(false)}
                >
                  취소
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={isProcessing}
                >
                  {isProcessing ? '처리 중...' : '발송 완료'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default OrderDetailPage;
