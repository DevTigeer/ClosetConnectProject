import { useState, useEffect } from 'react';
import { clothAPI, uploadAPI } from '../services/api';
import ClothCard from '../components/ClothCard';
import ClothDetailModal from '../components/ClothDetailModal';
import AddClothModal from '../components/AddClothModal';
import WeatherRecommend from '../components/WeatherRecommend';
import ClothingRecommend from '../components/ClothingRecommend';
import './ClosetPage.css';

const CATEGORIES = {
  TOP: '상의',
  BOTTOM: '하의',
  OUTER: '아우터',
  ONEPIECE: '원피스',
  SHOES: '신발',
  BAG: '가방',
  ACCESSORY: '액세서리',
  ETC: '기타',
};

function ClosetPage() {
  const [clothes, setClothes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedCloth, setSelectedCloth] = useState(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [deleteMode, setDeleteMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [recommendTab, setRecommendTab] = useState('weather'); // weather, personal, ai

  // 옷 목록 조회
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

  useEffect(() => {
    fetchClothes();
  }, []);

  // 카테고리별 그룹화
  const groupedClothes = Object.keys(CATEGORIES).reduce((acc, category) => {
    acc[category] = clothes.filter((cloth) => cloth.category === category);
    return acc;
  }, {});

  // 옷 추가
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

  // 옷 삭제
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

  return (
    <div className="closet-page">
      <header className="closet-header">
        <h1>옷장</h1>
        <div className="header-buttons">
          <button className="btn" onClick={toggleDeleteMode}>
            {deleteMode ? '삭제 실행' : '삭제 모드'}
          </button>
          <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
            옷 추가
          </button>
        </div>
      </header>

      {loading ? (
        <div className="loading">로딩 중...</div>
      ) : (
        <div className="closet-content">
          {/* 추천 섹션 */}
          <section className="recommend-section">
            <h2 className="section-title">오늘의 추천</h2>
            <div className="recommend-tabs">
              <button
                className={`recommend-tab ${recommendTab === 'weather' ? 'active' : ''}`}
                onClick={() => setRecommendTab('weather')}
              >
                🌤️ 오늘의 날씨
              </button>
              <button
                className={`recommend-tab ${recommendTab === 'personal' ? 'active' : ''}`}
                onClick={() => setRecommendTab('personal')}
              >
                ⭐ 나만의 추천
              </button>
              <button
                className={`recommend-tab ${recommendTab === 'ai' ? 'active' : ''}`}
                onClick={() => setRecommendTab('ai')}
              >
                🤖 AI 추천
              </button>
            </div>

            <div className="recommend-content">
              {recommendTab === 'weather' && <WeatherRecommend />}
              {recommendTab === 'personal' && <ClothingRecommend />}
              {recommendTab === 'ai' && (
                <div className="placeholder-recommend">
                  <p>AI 추천 기능은 곧 제공될 예정입니다.</p>
                </div>
              )}
            </div>
          </section>

          {/* 옷장 목록 */}
          <h2 className="section-title">내 옷장</h2>
          {Object.entries(CATEGORIES).map(([category, label]) => {
            const items = groupedClothes[category] || [];
            if (items.length === 0) return null;

            return (
              <section key={category} className="category-section">
                <h2 className="category-title">{label}</h2>
                <div className="cloth-grid">
                  {items.map((cloth) => (
                    <ClothCard
                      key={cloth.id}
                      cloth={cloth}
                      deleteMode={deleteMode}
                      isSelected={selectedIds.has(cloth.id)}
                      onToggleSelect={() => toggleSelection(cloth.id)}
                      onClick={() => !deleteMode && setSelectedCloth(cloth)}
                    />
                  ))}
                </div>
              </section>
            );
          })}

          {clothes.length === 0 && (
            <div className="empty-state">
              <p>등록된 옷이 없습니다.</p>
              <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
                첫 옷 추가하기
              </button>
            </div>
          )}
        </div>
      )}

      {selectedCloth && (
        <ClothDetailModal cloth={selectedCloth} onClose={() => setSelectedCloth(null)} />
      )}

      {showAddModal && (
        <AddClothModal onClose={() => setShowAddModal(false)} onSubmit={handleAddCloth} />
      )}
    </div>
  );
}

export default ClosetPage;
