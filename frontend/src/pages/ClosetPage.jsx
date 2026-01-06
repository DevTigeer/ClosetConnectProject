import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { clothAPI } from '../services/api';
import ClothCard from '../components/ClothCard';
import ClothDetailModal from '../components/ClothDetailModal';
import AddClothModal from '../components/AddClothModal';
import OutfitTryonModal from '../components/OutfitTryonModal';
import './ClosetPage.css';

const CATEGORIES = {
  TOP: '상의',
  BOTTOM: '하의',
  SHOES: '신발',
  ACC: '액세서리',
};

function ClosetPage() {
  const navigate = useNavigate();
  const token = localStorage.getItem('accessToken');
  const [clothes, setClothes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedCloth, setSelectedCloth] = useState(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showTryonModal, setShowTryonModal] = useState(false);
  const [deleteMode, setDeleteMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState(new Set());

  useEffect(() => {
    if (token) {
      fetchClothes();
    }
  }, [token]);

  const fetchClothes = async () => {
    setLoading(true);
    try {
      const response = await clothAPI.list({ page: 0, size: 1000 });
      setClothes(response.data.content || []);
    } catch (err) {
      console.error(err);
      alert('옷 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const groupedClothes = Object.keys(CATEGORIES).reduce((acc, category) => {
    acc[category] = clothes.filter((cloth) => cloth.category === category);
    return acc;
  }, {});

  const handleAddCloth = async (data) => {
    try {
      await clothAPI.create(data);
      setShowAddModal(false);
      fetchClothes();
    } catch (err) {
      console.error(err);
      throw err;
    }
  };

  const handleDelete = async () => {
    if (selectedIds.size === 0) {
      setDeleteMode(false);
      return;
    }

    if (!confirm(`${selectedIds.size}개의 옷을 삭제하시겠습니까?`)) {
      return;
    }

    try {
      await Promise.all([...selectedIds].map((id) => clothAPI.delete(id)));
      setSelectedIds(new Set());
      setDeleteMode(false);
      fetchClothes();
    } catch (err) {
      console.error(err);
      alert('삭제에 실패했습니다.');
    }
  };

  const toggleDeleteMode = () => {
    if (deleteMode) {
      handleDelete();
    } else {
      setDeleteMode(true);
      setSelectedIds(new Set());
    }
  };

  const toggleSelection = (id) => {
    const newSet = new Set(selectedIds);
    if (newSet.has(id)) {
      newSet.delete(id);
    } else {
      newSet.add(id);
    }
    setSelectedIds(newSet);
  };

  const handleSellCloth = (cloth) => {
    // 해당 옷으로 중고거래 등록 페이지로 이동
    navigate('/market/create', { state: { clothId: cloth.id, cloth } });
  };

  // 비로그인 시 UI
  if (!token) {
    return (
      <div className="closet-page">
        <div className="login-prompt-container">
          <div className="login-prompt-card">
            <div className="login-prompt-icon">👔</div>
            <h2 className="login-prompt-title">내 옷장을 시작하세요</h2>
            <p className="login-prompt-text">
              옷장을 체계적으로 관리하고<br/>
              날씨에 맞는 옷차림을 추천받으세요
            </p>
            <div className="login-prompt-buttons">
              <button onClick={() => navigate('/login')} className="btn-primary">
                로그인
              </button>
              <button onClick={() => navigate('/signup')} className="btn-secondary">
                회원가입
              </button>
            </div>
          </div>
          <div className="login-prompt-features">
            <div className="feature-item">
              <div className="feature-icon">📦</div>
              <div>
                <h3>옷장 관리</h3>
                <p>내 옷을 카테고리별로 정리하세요</p>
              </div>
            </div>
            <div className="feature-item">
              <div className="feature-icon">🌤️</div>
              <div>
                <h3>날씨 맞춤 추천</h3>
                <p>실시간 날씨에 맞는 코디 제안</p>
              </div>
            </div>
            <div className="feature-item">
              <div className="feature-icon">🛍️</div>
              <div>
                <h3>중고거래</h3>
                <p>안 입는 옷은 판매하고 필요한 옷은 구매</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // 로그인 후 UI
  return (
    <div className="closet-page">
      <div className="page-container">
        <header className="page-header">
          <div>
            <h1 className="page-title">내 옷장</h1>
            <p className="page-subtitle">총 {clothes.length}벌의 옷이 등록되어 있습니다</p>
          </div>
          <div className="header-actions">
            {clothes.length > 0 && (
              <>
                <button
                  className="btn-tryon"
                  onClick={() => setShowTryonModal(true)}
                >
                  🎨 조합하기
                </button>
                <button
                  className={`btn-secondary ${deleteMode ? 'active' : ''}`}
                  onClick={toggleDeleteMode}
                >
                  {deleteMode ? `삭제 실행 (${selectedIds.size}개)` : '삭제 모드'}
                </button>
              </>
            )}
            <button className="btn-primary" onClick={() => setShowAddModal(true)}>
              + 옷 추가
            </button>
          </div>
        </header>

        {loading ? (
          <div className="loading-state">
            <div className="spinner"></div>
            <p>로딩 중...</p>
          </div>
        ) : (
          <div className="closet-content">
            {Object.entries(CATEGORIES).map(([category, label]) => {
              const items = groupedClothes[category] || [];
              if (items.length === 0) return null;

              return (
                <section key={category} className="category-section">
                  <div className="category-header">
                    <h2 className="category-title">{label}</h2>
                    <span className="category-count">{items.length}벌</span>
                  </div>
                  <div className="cloth-grid">
                    {items.map((cloth) => (
                      <ClothCard
                        key={cloth.id}
                        cloth={cloth}
                        deleteMode={deleteMode}
                        isSelected={selectedIds.has(cloth.id)}
                        onToggleSelect={() => toggleSelection(cloth.id)}
                        onClick={() => !deleteMode && setSelectedCloth(cloth)}
                        onSell={() => handleSellCloth(cloth)}
                      />
                    ))}
                  </div>
                </section>
              );
            })}

            {clothes.length === 0 && (
              <div className="empty-state">
                <div className="empty-icon">👔</div>
                <h3 className="empty-title">아직 등록된 옷이 없습니다</h3>
                <p className="empty-text">
                  첫 옷을 등록하고 스마트한 옷장 관리를 시작하세요
                </p>
                <button className="btn-primary" onClick={() => setShowAddModal(true)}>
                  첫 옷 추가하기
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {selectedCloth && (
        <ClothDetailModal
          cloth={selectedCloth}
          onClose={() => setSelectedCloth(null)}
          onSell={() => {
            setSelectedCloth(null);
            handleSellCloth(selectedCloth);
          }}
        />
      )}

      {showAddModal && (
        <AddClothModal onClose={() => setShowAddModal(false)} onSubmit={handleAddCloth} />
      )}

      {showTryonModal && (
        <OutfitTryonModal clothes={clothes} onClose={() => setShowTryonModal(false)} />
      )}
    </div>
  );
}

export default ClosetPage;
