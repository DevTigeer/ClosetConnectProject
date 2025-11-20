import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { marketAPI } from '../services/api';
import { getImageUrl, handleImageError } from '../utils/imageUtils';
import './MyLikesPage.css';

function MyLikesPage() {
  const navigate = useNavigate();
  const token = localStorage.getItem('accessToken');
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  useEffect(() => {
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }
    fetchLikedProducts(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // 컴포넌트 마운트 시 한 번만 실행

  const fetchLikedProducts = async (pageNum) => {
    setLoading(true);
    try {
      const response = await marketAPI.getLiked({ page: pageNum, size: 20 });

      if (pageNum === 0) {
        setProducts(response.data.content || []);
      } else {
        setProducts(prev => [...prev, ...(response.data.content || [])]);
      }

      setHasMore(!response.data.last);
      setPage(pageNum);
    } catch (error) {
      console.error('찜 목록 조회 실패:', error);
      setProducts([]);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'RESERVED':
        return <span className="status-badge reserved">예약중</span>;
      case 'SOLD':
        return <span className="status-badge sold">거래완료</span>;
      default:
        return null;
    }
  };

  return (
    <div className="my-likes-page">
      <div className="page-container">
        <header className="page-header">
          <h1 className="page-title">찜 목록</h1>
          <p className="page-subtitle">마음에 드는 상품을 모아보세요</p>
        </header>

        {loading && page === 0 ? (
          <div className="loading-state">
            <div className="spinner"></div>
            <p>로딩 중...</p>
          </div>
        ) : (
          <>
            {products.length > 0 ? (
              <>
                <div className="products-grid">
                  {products.map((product) => (
                    <div
                      key={product.productId}
                      className="product-card"
                      onClick={() => navigate(`/market/products/${product.productId}`)}
                    >
                      <div className="product-image-wrapper">
                        <img
                          src={getImageUrl(product.imageUrl)}
                          alt={product.title}
                          className="product-image"
                          onError={handleImageError}
                        />
                        {product.status !== 'ON_SALE' && (
                          <div className="product-status-overlay">
                            {getStatusBadge(product.status)}
                          </div>
                        )}
                      </div>
                      <div className="product-info">
                        <h3 className="product-title">{product.title}</h3>
                        <p className="product-price">
                          {product.price?.toLocaleString()}원
                        </p>
                        <div className="product-meta">
                          <span className="product-region">{product.region}</span>
                          <div className="product-stats">
                            <span>❤️ {product.likeCount || 0}</span>
                            <span>💬 {product.commentCount || 0}</span>
                            <span>👁️ {product.viewCount || 0}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                {hasMore && (
                  <button
                    className="load-more-btn"
                    onClick={() => fetchLikedProducts(page + 1)}
                    disabled={loading}
                  >
                    {loading ? '로딩 중...' : '더 보기'}
                  </button>
                )}
              </>
            ) : (
              <div className="empty-state">
                <div className="empty-icon">💝</div>
                <h3 className="empty-title">찜한 상품이 없습니다</h3>
                <p className="empty-text">
                  마음에 드는 상품에 하트를 눌러 찜 목록에 추가하세요
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

export default MyLikesPage;
