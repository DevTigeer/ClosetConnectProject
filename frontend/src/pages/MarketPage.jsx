import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { marketAPI } from '../services/api';
import { getProductImageUrl, handleImageError } from '../utils/imageUtils';
import './MarketPage.css';

function MarketPage() {
  const navigate = useNavigate();
  const token = localStorage.getItem('accessToken');
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({
    status: '',
    region: '',
    keyword: '',
    sort: 'LATEST',
  });

  useEffect(() => {
    fetchProducts();
  }, [filters]);

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const params = {};
      if (filters.status) params.status = filters.status;
      if (filters.region) params.region = filters.region;
      if (filters.keyword) params.keyword = filters.keyword;
      params.sort = filters.sort;

      const response = await marketAPI.list(params);
      setProducts(response.data.content || []);
    } catch (error) {
      console.error('상품 목록 로드 실패:', error);
      // 에러가 발생해도 빈 배열로 설정
      setProducts([]);
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  const handleSearch = (e) => {
    e.preventDefault();
    fetchProducts();
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
    <div className="market-page">
      <div className="page-container">
        {/* 헤더 */}
        <header className="page-header">
          <div>
            <h1 className="page-title">중고거래</h1>
            <p className="page-subtitle">
              안 입는 옷은 판매하고, 필요한 옷은 합리적으로 구매하세요
            </p>
          </div>
          {token && (
            <button
              className="btn-primary"
              onClick={() => navigate('/market/create')}
            >
              + 상품 등록
            </button>
          )}
        </header>

        {/* 필터 */}
        <div className="market-filters">
          <form onSubmit={handleSearch} className="search-form">
            <input
              type="text"
              placeholder="상품명 검색..."
              value={filters.keyword}
              onChange={(e) => handleFilterChange('keyword', e.target.value)}
              className="search-input"
            />
            <button type="submit" className="search-btn">
              검색
            </button>
          </form>

          <div className="filter-group">
            <select
              value={filters.status}
              onChange={(e) => handleFilterChange('status', e.target.value)}
              className="filter-select"
            >
              <option value="">전체 상태</option>
              <option value="ON_SALE">판매중</option>
              <option value="RESERVED">예약중</option>
              <option value="SOLD">거래완료</option>
            </select>

            <select
              value={filters.sort}
              onChange={(e) => handleFilterChange('sort', e.target.value)}
              className="filter-select"
            >
              <option value="LATEST">최신순</option>
              <option value="PRICE_LOW">낮은가격순</option>
              <option value="PRICE_HIGH">높은가격순</option>
            </select>
          </div>
        </div>

        {/* 상품 목록 */}
        {loading ? (
          <div className="loading-state">
            <div className="spinner"></div>
            <p>로딩 중...</p>
          </div>
        ) : (
          <>
            {products.length > 0 ? (
              <div className="products-grid">
                {products.map((product) => (
                  <div
                    key={product.productId}
                    className="product-card"
                    onClick={() => navigate(`/market/products/${product.productId}`)}
                  >
                    <div className="product-image-wrapper">
                      <img
                        src={getProductImageUrl(product)}
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
            ) : (
              <div className="empty-state">
                <div className="empty-icon">🛍️</div>
                <h3 className="empty-title">등록된 상품이 없습니다</h3>
                <p className="empty-text">
                  {token
                    ? '첫 상품을 등록하고 중고거래를 시작하세요'
                    : '로그인 후 상품을 등록할 수 있습니다'}
                </p>
                {token && (
                  <button
                    className="btn-primary"
                    onClick={() => navigate('/market/create')}
                  >
                    첫 상품 등록하기
                  </button>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default MarketPage;
