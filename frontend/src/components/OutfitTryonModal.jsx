import { useState } from 'react';
import PropTypes from 'prop-types';
import { outfitAPI, ootdAPI } from '../services/api';
import { getClothImageUrl } from '../utils/imageUtils';
import './OutfitTryonModal.css';

const CATEGORIES = {
  TOP: { key: 'TOP', label: '상의', emoji: '👕', max: 1 },
  BOTTOM: { key: 'BOTTOM', label: '하의', emoji: '👖', max: 1 },
  SHOES: { key: 'SHOES', label: '신발', emoji: '👟', max: 1 },
  ACC: { key: 'ACC', label: '액세서리', emoji: '👜', max: 10 },
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const SNS_SHARE_CONFIG = {
  POPUP_SIZE: 'width=600,height=400',
};

function OutfitTryonModal({ clothes, onClose }) {
  const [selectedItems, setSelectedItems] = useState({
    TOP: null,
    BOTTOM: null,
    SHOES: null,
    ACC: [],
  });
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  // 카테고리별 옷 그룹화
  const groupedClothes = Object.keys(CATEGORIES).reduce((acc, category) => {
    acc[category] = clothes.filter((cloth) => cloth.category === category);
    return acc;
  }, {});

  const handleSelect = (category, cloth) => {
    if (category === 'ACC') {
      // 악세서리는 여러 개 선택 가능
      const current = selectedItems.ACC;
      const isSelected = current.some((item) => item.id === cloth.id);

      if (isSelected) {
        setSelectedItems({
          ...selectedItems,
          ACC: current.filter((item) => item.id !== cloth.id),
        });
      } else if (current.length < CATEGORIES.ACC.max) {
        setSelectedItems({
          ...selectedItems,
          ACC: [...current, cloth],
        });
      }
    } else {
      // 단일 선택
      setSelectedItems({
        ...selectedItems,
        [category]: selectedItems[category]?.id === cloth.id ? null : cloth,
      });
    }
  };

  const isSelected = (category, cloth) => {
    if (category === 'ACC') {
      return selectedItems.ACC.some((item) => item.id === cloth.id);
    }
    return selectedItems[category]?.id === cloth.id;
  };

  const canGenerate = () => {
    // 최소 1개 이상 선택되어야 함
    return (
      selectedItems.TOP ||
      selectedItems.BOTTOM ||
      selectedItems.SHOES ||
      selectedItems.ACC.length > 0
    );
  };

  const handleGenerate = async () => {
    if (!canGenerate()) {
      alert('최소 1개 이상의 아이템을 선택해주세요.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const requestData = {
        upperClothesId: selectedItems.TOP?.id || null,
        lowerClothesId: selectedItems.BOTTOM?.id || null,
        shoesId: selectedItems.SHOES?.id || null,
        accessoriesIds: selectedItems.ACC.map((item) => item.id),
      };

      const response = await outfitAPI.tryon(requestData);

      if (response.data.success) {
        setResult(response.data);
      } else {
        setError(response.data.message || 'Try-on 생성에 실패했습니다.');
      }
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Try-on 생성 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setSelectedItems({
      TOP: null,
      BOTTOM: null,
      SHOES: null,
      ACC: [],
    });
    setResult(null);
    setError(null);
  };

  // 이미지 다운로드
  const handleDownload = async () => {
    try {
      const imageUrl = `${API_BASE_URL}${result.imageUrl}`;
      const response = await fetch(imageUrl);
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `outfit-tryon-${Date.now()}.png`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('다운로드 실패:', err);
      alert('이미지 다운로드에 실패했습니다.');
    }
  };

  // SNS 공유
  const handleShare = (platform) => {
    const shareUrl = window.location.href;
    const shareText = 'ClosetConnect에서 만든 나만의 코디를 확인해보세요!';

    switch (platform) {
      case 'facebook':
        window.open(
          `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(shareUrl)}`,
          '_blank',
          SNS_SHARE_CONFIG.POPUP_SIZE
        );
        break;
      case 'twitter':
        window.open(
          `https://twitter.com/intent/tweet?text=${encodeURIComponent(shareText)}&url=${encodeURIComponent(shareUrl)}`,
          '_blank',
          SNS_SHARE_CONFIG.POPUP_SIZE
        );
        break;
      case 'naver':
        window.open(
          `https://share.naver.com/web/shareView?url=${encodeURIComponent(shareUrl)}&title=${encodeURIComponent(shareText)}`,
          '_blank',
          SNS_SHARE_CONFIG.POPUP_SIZE
        );
        break;
      case 'instagram':
        alert('Instagram은 모바일 앱에서만 공유가 가능합니다.\n이미지를 다운로드한 후 Instagram 앱에서 직접 업로드해주세요.');
        handleDownload();
        break;
      case 'kakao':
        alert('카카오톡 공유 기능은 준비 중입니다.\n이미지를 다운로드한 후 카카오톡에서 직접 공유해주세요.');
        handleDownload();
        break;
      default:
        break;
    }
  };

  const getTotalSelected = () => {
    return (
      (selectedItems.TOP ? 1 : 0) +
      (selectedItems.BOTTOM ? 1 : 0) +
      (selectedItems.SHOES ? 1 : 0) +
      selectedItems.ACC.length
    );
  };

  // OOTD 저장
  const handleSave = async () => {
    try {
      setSaving(true);
      await ootdAPI.save({
        imageUrl: result.imageUrl,
        description: null,
      });
      alert('OOTD가 저장되었습니다! 🎉');
    } catch (err) {
      console.error('OOTD 저장 실패:', err);
      alert('OOTD 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  // 결과가 있으면 결과 화면 표시
  if (result) {
    return (
      <div className="modal-overlay" onClick={onClose}>
        <div className="outfit-tryon-modal result" onClick={(e) => e.stopPropagation()}>
          <div className="modal-header">
            <h2>✨ Try-On 결과</h2>
            <button className="close-btn" onClick={onClose}>×</button>
          </div>

          <div className="result-content">
            <div className="result-image-container">
              <img
                src={`${API_BASE_URL}${result.imageUrl}`}
                alt="Try-on result"
                className="result-image"
              />
              <div className="result-badge">
                <span className="badge-icon">🤖</span>
                <span className="badge-text">Powered by {result.engine}</span>
              </div>
            </div>

            <div className="result-actions">
              <button className="btn-download" onClick={handleDownload}>
                <span>⬇️</span>
                다운로드
              </button>

              <div className="share-dropdown">
                <button className="btn-share">
                  <span>📤</span>
                  공유하기
                </button>
                <div className="share-menu">
                  <button onClick={() => handleShare('facebook')}>
                    <span>📘</span>
                    Facebook
                  </button>
                  <button onClick={() => handleShare('twitter')}>
                    <span>🐦</span>
                    Twitter
                  </button>
                  <button onClick={() => handleShare('naver')}>
                    <span>🟢</span>
                    네이버 블로그
                  </button>
                  <button onClick={() => handleShare('instagram')}>
                    <span>📷</span>
                    Instagram
                  </button>
                  <button onClick={() => handleShare('kakao')}>
                    <span>💬</span>
                    카카오톡
                  </button>
                </div>
              </div>
            </div>

            <div className="result-actions-secondary">
              <button className="btn-secondary" onClick={handleReset}>
                다시 조합하기
              </button>
              <button
                className="btn-primary"
                onClick={handleSave}
                disabled={saving}
              >
                {saving ? (
                  <>
                    <span className="spinner-small"></span>
                    저장 중...
                  </>
                ) : (
                  <>
                    <span>💾</span>
                    OOTD 저장
                  </>
                )}
              </button>
              <button className="btn-primary" onClick={onClose}>
                완료
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // 선택 화면
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="outfit-tryon-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h2>🎨 나만의 코디 조합하기</h2>
            <p className="modal-subtitle">
              원하는 아이템을 선택하고 AI가 조합한 모습을 확인해보세요
            </p>
          </div>
          <button className="close-btn" onClick={onClose}>×</button>
        </div>

        <div className="selection-summary">
          <div className="summary-item">
            <span className="summary-label">선택된 아이템</span>
            <span className="summary-value">{getTotalSelected()}개</span>
          </div>
          {Object.entries(selectedItems).map(([category, item]) => {
            if (category === 'ACC') {
              if (item.length === 0) return null;
              return (
                <div key={category} className="summary-item">
                  <span className="summary-label">{CATEGORIES[category].emoji} {CATEGORIES[category].label}</span>
                  <span className="summary-value">{item.length}개</span>
                </div>
              );
            }
            if (!item) return null;
            return (
              <div key={category} className="summary-item">
                <span className="summary-label">{CATEGORIES[category].emoji} {CATEGORIES[category].label}</span>
                <span className="summary-value">선택됨</span>
              </div>
            );
          })}
        </div>

        <div className="modal-content">
          {Object.entries(CATEGORIES).map(([category, config]) => {
            const items = groupedClothes[category] || [];
            if (items.length === 0) return null;

            return (
              <div key={category} className="category-section">
                <div className="category-header">
                  <h3 className="category-title">
                    <span className="category-emoji">{config.emoji}</span>
                    {config.label}
                  </h3>
                  <span className="category-info">
                    {category === 'ACC'
                      ? `${selectedItems[category].length}/${config.max}개 선택`
                      : selectedItems[category] ? '선택됨' : '선택 안 함'
                    }
                  </span>
                </div>

                <div className="cloth-grid">
                  {items.map((cloth) => (
                    <div
                      key={cloth.id}
                      className={`cloth-item ${isSelected(category, cloth) ? 'selected' : ''}`}
                      onClick={() => handleSelect(category, cloth)}
                    >
                      <div className="cloth-image-wrapper">
                        <img
                          src={getClothImageUrl(cloth)}
                          alt={cloth.name}
                          className="cloth-image"
                        />
                        {isSelected(category, cloth) && (
                          <div className="selected-overlay">
                            <div className="checkmark">✓</div>
                          </div>
                        )}
                      </div>
                      <p className="cloth-name">{cloth.name}</p>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>

        {error && (
          <div className="error-message">
            <span className="error-icon">⚠️</span>
            {error}
          </div>
        )}

        <div className="modal-footer">
          <button className="btn-secondary" onClick={onClose}>
            취소
          </button>
          <button
            className="btn-primary"
            onClick={handleGenerate}
            disabled={!canGenerate() || loading}
          >
            {loading ? (
              <>
                <span className="spinner-small"></span>
                생성 중...
              </>
            ) : (
              <>
                <span>✨</span>
                조합 생성하기
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

OutfitTryonModal.propTypes = {
  clothes: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      name: PropTypes.string.isRequired,
      category: PropTypes.string.isRequired,
      imageUrl: PropTypes.string,
      segmentedImageUrl: PropTypes.string,
      inpaintedImageUrl: PropTypes.string,
    })
  ).isRequired,
  onClose: PropTypes.func.isRequired,
};

export default OutfitTryonModal;
