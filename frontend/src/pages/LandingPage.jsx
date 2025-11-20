import { useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { getProductImageUrl, handleImageError } from '../utils/imageUtils';
import './LandingPage.css';

function LandingPage() {
  const navigate = useNavigate();
  const [weatherData, setWeatherData] = useState(null);
  const [recommendedProducts, setRecommendedProducts] = useState([]);
  const [popularPosts, setPopularPosts] = useState([]);

  useEffect(() => {
    // 날씨 정보 가져오기
    fetch('/api/v1/weather/city/seoul')
      .then(res => res.json())
      .then(data => setWeatherData(data))
      .catch(err => console.error('날씨 정보 로드 실패:', err));

    // 인기 중고거래 상품 가져오기
    fetch('/api/v1/market/products?sort=likes&size=6')
      .then(res => res.json())
      .then(data => setRecommendedProducts(data.content || []))
      .catch(err => console.error('상품 정보 로드 실패:', err));

    // 인기 커뮤니티 게시글 가져오기
    fetch('/api/v1/posts?sort=likes&size=6')
      .then(res => res.json())
      .then(data => setPopularPosts(data.content || []))
      .catch(err => console.error('게시글 정보 로드 실패:', err));
  }, []);

  return (
    <div className="landing-page">
      {/* 상단 네비게이션 */}
      <header className="landing-header">
        <div className="container">
          <div className="header-content">
            <h1 className="logo">ClosetConnect</h1>
            <div className="header-search">
              <input
                type="text"
                placeholder="옷, 스타일, 커뮤니티 검색..."
                className="search-input"
              />
              <button className="search-btn">검색</button>
            </div>
            <nav className="header-nav">
              <button onClick={() => navigate('/login')} className="nav-link">로그인</button>
              <button onClick={() => navigate('/signup')} className="nav-btn-primary">회원가입</button>
            </nav>
          </div>
        </div>
      </header>

      {/* 히어로 배너 */}
      <section className="hero-section">
        <div className="container">
          <div className="hero-content">
            <h2 className="hero-title">당신의 옷장을 스마트하게</h2>
            <p className="hero-subtitle">
              옷장 관리부터 스타일 추천, 중고거래까지<br/>
              모든 패션 라이프를 한 곳에서
            </p>
            <div className="hero-cta">
              <button onClick={() => navigate('/signup')} className="cta-primary">
                무료로 시작하기
              </button>
              <button onClick={() => navigate('/community')} className="cta-secondary">
                커뮤니티 둘러보기
              </button>
            </div>
          </div>
          {weatherData && (
            <div className="hero-weather-card">
              <div className="weather-icon">{weatherData.current?.weatherEmoji || '☀️'}</div>
              <div className="weather-info">
                <h3>오늘의 날씨</h3>
                <p className="weather-temp">{weatherData.current?.temperature}°C</p>
                <p className="weather-desc">{weatherData.current?.weatherDescription}</p>
                <button onClick={() => navigate('/recommend')} className="weather-cta">
                  날씨 맞춤 추천 보기
                </button>
              </div>
            </div>
          )}
        </div>
      </section>

      {/* 핵심 기능 소개 */}
      <section className="features-section">
        <div className="container">
          <h2 className="section-title">핵심 기능</h2>
          <div className="features-grid">
            <div className="feature-card" onClick={() => navigate('/closet')}>
              <div className="feature-icon">👔</div>
              <h3>옷장 관리</h3>
              <p>내 옷을 체계적으로 관리하고 스타일을 찾아보세요</p>
            </div>
            <div className="feature-card" onClick={() => navigate('/recommend')}>
              <div className="feature-icon">🌤️</div>
              <h3>날씨 맞춤 추천</h3>
              <p>실시간 날씨에 맞는 완벽한 코디를 추천받으세요</p>
            </div>
            <div className="feature-card" onClick={() => navigate('/market')}>
              <div className="feature-icon">🛍️</div>
              <h3>중고거래</h3>
              <p>안 입는 옷은 판매하고, 필요한 옷은 합리적으로 구매하세요</p>
            </div>
            <div className="feature-card" onClick={() => navigate('/community')}>
              <div className="feature-icon">💬</div>
              <h3>패션 커뮤니티</h3>
              <p>스타일 팁을 공유하고 패션 인사이트를 얻으세요</p>
            </div>
          </div>
        </div>
      </section>

      {/* 인기 중고거래 상품 */}
      <section className="products-section">
        <div className="container">
          <div className="section-header">
            <h2 className="section-title">인기 중고거래</h2>
            <button onClick={() => navigate('/market')} className="section-link">
              전체보기 →
            </button>
          </div>
          <div className="products-grid">
            {recommendedProducts.length > 0 ? (
              recommendedProducts.map((product) => (
                <div
                  key={product.productId}
                  className="product-card"
                  onClick={() => navigate(`/market/products/${product.productId}`)}
                >
                  <div className="product-image">
                    <img
                      src={getProductImageUrl(product)}
                      alt={product.title}
                      onError={handleImageError}
                    />
                    {product.status !== 'SALE' && (
                      <div className="product-badge">{product.status === 'RESERVED' ? '예약중' : '거래완료'}</div>
                    )}
                  </div>
                  <div className="product-info">
                    <h3 className="product-title">{product.title}</h3>
                    <p className="product-price">{product.price?.toLocaleString()}원</p>
                    <div className="product-meta">
                      <span>❤️ {product.likeCount || 0}</span>
                      <span>💬 {product.commentCount || 0}</span>
                    </div>
                  </div>
                </div>
              ))
            ) : (
              <div className="empty-state">
                <p>등록된 상품이 없습니다</p>
                <button onClick={() => navigate('/market/create')} className="cta-primary">
                  첫 상품 등록하기
                </button>
              </div>
            )}
          </div>
        </div>
      </section>

      {/* 인기 커뮤니티 게시글 */}
      <section className="community-section">
        <div className="container">
          <div className="section-header">
            <h2 className="section-title">인기 커뮤니티</h2>
            <button onClick={() => navigate('/community')} className="section-link">
              전체보기 →
            </button>
          </div>
          <div className="posts-grid">
            {popularPosts.length > 0 ? (
              popularPosts.map((post) => (
                <div
                  key={post.postId}
                  className="post-card"
                  onClick={() => navigate(`/community/${post.boardSlug}/posts/${post.postId}`)}
                >
                  {post.attachments && post.attachments.length > 0 && (
                    <div className="post-image">
                      <img
                        src={post.attachments[0].fileUrl}
                        alt={post.title}
                        onError={(e) => { e.target.src = '/placeholder-post.jpg'; }}
                      />
                    </div>
                  )}
                  <div className="post-info">
                    <div className="post-category">{post.boardName}</div>
                    <h3 className="post-title">{post.title}</h3>
                    <p className="post-excerpt">{post.content?.substring(0, 80)}...</p>
                    <div className="post-meta">
                      <span className="post-author">{post.authorNickname}</span>
                      <span>❤️ {post.likeCount || 0}</span>
                      <span>💬 {post.commentCount || 0}</span>
                    </div>
                  </div>
                </div>
              ))
            ) : (
              <div className="empty-state">
                <p>게시글이 없습니다</p>
                <button onClick={() => navigate('/community')} className="cta-primary">
                  커뮤니티 시작하기
                </button>
              </div>
            )}
          </div>
        </div>
      </section>

      {/* 카테고리 탐색 */}
      <section className="categories-section">
        <div className="container">
          <h2 className="section-title">카테고리 탐색</h2>
          <div className="categories-grid">
            <div className="category-card" onClick={() => navigate('/closet?category=TOP')}>
              <div className="category-icon">👕</div>
              <h3>상의</h3>
            </div>
            <div className="category-card" onClick={() => navigate('/closet?category=BOTTOM')}>
              <div className="category-icon">👖</div>
              <h3>하의</h3>
            </div>
            <div className="category-card" onClick={() => navigate('/closet?category=OUTER')}>
              <div className="category-icon">🧥</div>
              <h3>아우터</h3>
            </div>
            <div className="category-card" onClick={() => navigate('/closet?category=SHOES')}>
              <div className="category-icon">👟</div>
              <h3>신발</h3>
            </div>
            <div className="category-card" onClick={() => navigate('/closet?category=ACC')}>
              <div className="category-icon">👒</div>
              <h3>액세서리</h3>
            </div>
            <div className="category-card" onClick={() => navigate('/closet')}>
              <div className="category-icon">📦</div>
              <h3>전체보기</h3>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="landing-footer">
        <div className="container">
          <div className="footer-grid">
            <div className="footer-section">
              <h3>ClosetConnect</h3>
              <p>스마트한 패션 라이프의 시작</p>
            </div>
            <div className="footer-section">
              <h4>서비스</h4>
              <ul>
                <li><button onClick={() => navigate('/closet')}>옷장 관리</button></li>
                <li><button onClick={() => navigate('/recommend')}>날씨 추천</button></li>
                <li><button onClick={() => navigate('/market')}>중고거래</button></li>
                <li><button onClick={() => navigate('/community')}>커뮤니티</button></li>
              </ul>
            </div>
            <div className="footer-section">
              <h4>고객지원</h4>
              <ul>
                <li><a href="#">공지사항</a></li>
                <li><a href="#">FAQ</a></li>
                <li><a href="#">문의하기</a></li>
              </ul>
            </div>
            <div className="footer-section">
              <h4>회사정보</h4>
              <ul>
                <li><a href="#">회사소개</a></li>
                <li><a href="#">이용약관</a></li>
                <li><a href="#">개인정보처리방침</a></li>
              </ul>
            </div>
          </div>
          <div className="footer-bottom">
            <p>© 2024 ClosetConnect. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default LandingPage;
