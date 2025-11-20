import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { clothAPI, marketAPI } from '../services/api';
import { getClothImageUrl, handleImageError } from '../utils/imageUtils';
import './MarketCreatePage.css';

const PRODUCT_CONDITIONS = [
  { value: 'EXCELLENT', label: '상 (거의 새것)' },
  { value: 'GOOD', label: '중 (사용감 있음)' },
  { value: 'FAIR', label: '하 (손상 있음)' },
];

function MarketCreatePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const token = localStorage.getItem('accessToken');

  const [clothes, setClothes] = useState([]);
  const [selectedCloth, setSelectedCloth] = useState(null);
  const [formData, setFormData] = useState({
    title: '',
    price: '',
    description: '',
    productCondition: 'GOOD',
    region: '',
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    fetchClothes();

    // ClosetPage에서 전달받은 옷 정보가 있으면 자동 선택
    if (location.state?.cloth) {
      const cloth = location.state.cloth;
      setSelectedCloth(cloth);
      setFormData(prev => ({
        ...prev,
        title: cloth.name || '',
      }));
    }
  }, []);

  const fetchClothes = async () => {
    try {
      const response = await clothAPI.list({ page: 0, size: 1000 });
      setClothes(response.data.content || []);
    } catch (error) {
      console.error('옷 목록 조회 실패:', error);
    }
  };

  const handleClothSelect = (cloth) => {
    setSelectedCloth(cloth);
    setFormData(prev => ({
      ...prev,
      title: cloth.name || prev.title,
    }));
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!selectedCloth) {
      alert('판매할 옷을 선택해주세요.');
      return;
    }

    if (!formData.title.trim()) {
      alert('상품명을 입력해주세요.');
      return;
    }

    if (!formData.price || formData.price <= 0) {
      alert('올바른 가격을 입력해주세요.');
      return;
    }

    if (!formData.region.trim()) {
      alert('거래 지역을 입력해주세요.');
      return;
    }

    setLoading(true);
    try {
      const data = {
        clothId: selectedCloth.id,
        title: formData.title,
        price: parseInt(formData.price),
        description: formData.description,
        productCondition: formData.productCondition,
        region: formData.region,
      };

      const response = await marketAPI.create(data);
      alert('상품이 등록되었습니다!');
      navigate(`/market/products/${response.data.productId}`);
    } catch (error) {
      console.error('상품 등록 실패:', error);
      alert(error.response?.data?.message || '상품 등록에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="market-create-page">
      <div className="page-container">
        <button onClick={() => navigate('/market')} className="back-btn">
          ← 취소
        </button>

        <header className="page-header">
          <h1 className="page-title">상품 등록</h1>
          <p className="page-subtitle">옷장에서 판매할 옷을 선택하고 정보를 입력하세요</p>
        </header>

        <div className="create-layout">
          {/* 옷 선택 */}
          <div className="cloth-selection-section">
            <h2 className="section-title">판매할 옷 선택</h2>
            {clothes.length > 0 ? (
              <div className="clothes-grid">
                {clothes.map((cloth) => (
                  <div
                    key={cloth.id}
                    className={`cloth-select-card ${selectedCloth?.id === cloth.id ? 'selected' : ''}`}
                    onClick={() => handleClothSelect(cloth)}
                  >
                    <img
                      src={getClothImageUrl(cloth)}
                      alt={cloth.name}
                      className="cloth-select-image"
                      onError={handleImageError}
                    />
                    <div className="cloth-select-info">
                      <span className="cloth-select-name">{cloth.name}</span>
                      <span className="cloth-select-category">{cloth.category}</span>
                    </div>
                    {selectedCloth?.id === cloth.id && (
                      <div className="selected-badge">✓</div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="empty-clothes">
                <p>등록된 옷이 없습니다.</p>
                <button onClick={() => navigate('/closet')} className="btn-primary">
                  옷장으로 이동
                </button>
              </div>
            )}
          </div>

          {/* 상품 정보 입력 */}
          <div className="product-form-section">
            <h2 className="section-title">상품 정보</h2>
            <form onSubmit={handleSubmit} className="product-form">
              <div className="form-group">
                <label className="form-label">상품명 *</label>
                <input
                  type="text"
                  name="title"
                  value={formData.title}
                  onChange={handleChange}
                  placeholder="예: 나이키 반팔티"
                  className="form-input"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">가격 (원) *</label>
                <input
                  type="number"
                  name="price"
                  value={formData.price}
                  onChange={handleChange}
                  placeholder="예: 25000"
                  className="form-input"
                  min="0"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">상품 상태 *</label>
                <select
                  name="productCondition"
                  value={formData.productCondition}
                  onChange={handleChange}
                  className="form-select"
                  required
                >
                  {PRODUCT_CONDITIONS.map((condition) => (
                    <option key={condition.value} value={condition.value}>
                      {condition.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">거래 지역 *</label>
                <input
                  type="text"
                  name="region"
                  value={formData.region}
                  onChange={handleChange}
                  placeholder="예: 서울 강남구"
                  className="form-input"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">상품 설명</label>
                <textarea
                  name="description"
                  value={formData.description}
                  onChange={handleChange}
                  placeholder="상품에 대한 설명을 입력해주세요"
                  className="form-textarea"
                  rows="6"
                />
              </div>

              <div className="form-actions">
                <button
                  type="button"
                  onClick={() => navigate('/market')}
                  className="btn-secondary"
                  disabled={loading}
                >
                  취소
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={loading || !selectedCloth}
                >
                  {loading ? '등록 중...' : '상품 등록'}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}

export default MarketCreatePage;
