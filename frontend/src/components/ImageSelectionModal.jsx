import { useState } from 'react';
import { clothAPI } from '../services/api';
import { getImageUrl } from '../utils/imageUtils';
import './ImageSelectionModal.css';

const CATEGORIES = [
  { value: 'TOP', label: '상의' },
  { value: 'BOTTOM', label: '하의' },
  { value: 'OUTER', label: '아우터' },
  { value: 'ONEPIECE', label: '원피스' },
  { value: 'SHOES', label: '신발' },
  { value: 'BAG', label: '가방' },
  { value: 'ACCESSORY', label: '액세서리' },
  { value: 'ETC', label: '기타' },
];

const IMAGE_TYPES = [
  {
    type: 'ORIGINAL',
    label: '원본 이미지',
    description: '업로드한 원본 그대로',
    urlKey: 'originalImageUrl'
  },
  {
    type: 'REMOVED_BG',
    label: '배경 제거',
    description: '배경을 제거한 이미지',
    urlKey: 'removedBgImageUrl'
  },
  {
    type: 'SEGMENTED',
    label: '옷 영역 추출',
    description: '옷 부분만 크롭',
    urlKey: 'segmentedImageUrl'
  },
];

// AI 라벨을 한글로 변환
const translateLabel = (label) => {
  const labelMap = {
    'hat': '모자',
    'upper-clothes': '상의',
    'skirt': '치마',
    'pants': '바지',
    'dress': '원피스',
    'belt': '벨트',
    'left-shoe': '신발 (왼쪽)',
    'right-shoe': '신발 (오른쪽)',
    'bag': '가방',
    'scarf': '스카프',
    'sunglasses': '선글라스',
    'left-leg': '왼쪽 다리',
    'right-leg': '오른쪽 다리',
    'left-arm': '왼쪽 팔',
    'right-arm': '오른쪽 팔',
  };

  return labelMap[label] || label;
};

function ImageSelectionModal({ cloth, onClose, onConfirm }) {
  const [selectedType, setSelectedType] = useState('SEGMENTED'); // 기본값: 옷 영역 추출
  const [selectedImageUrl, setSelectedImageUrl] = useState(null); // 추가 아이템 선택 시 직접 URL 저장
  const [category, setCategory] = useState(cloth.suggestedCategory || cloth.category || 'TOP');
  const [loading, setLoading] = useState(false);

  const handleConfirm = async () => {
    if (!selectedType && !selectedImageUrl) {
      alert('이미지를 선택해주세요.');
      return;
    }

    setLoading(true);
    try {
      // selectedImageUrl이 있으면 직접 URL 전송, 없으면 타입 전송
      const payload = selectedImageUrl
        ? { selectedImageUrl: selectedImageUrl, category: category }
        : { selectedImageType: selectedType, category: category };

      await clothAPI.confirmImage(cloth.id, payload);

      alert('옷이 옷장에 추가되었습니다!');

      if (onConfirm) {
        onConfirm(cloth.id);  // clothId 전달
      }

      onClose();
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.message || '확정에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = async () => {
    // 닫기 버튼 클릭 시 기본 이미지(SEGMENTED)로 자동 확정
    if (!loading) {
      setLoading(true);
      try {
        await clothAPI.confirmImage(cloth.id, {
          selectedImageType: 'SEGMENTED',
          category: cloth.suggestedCategory || cloth.category || 'TOP'
        });

        if (onConfirm) {
          onConfirm(cloth.id);  // clothId 전달
        }

        onClose();
      } catch (err) {
        console.error(err);
        onClose(); // 에러 발생해도 닫기
      } finally {
        setLoading(false);
      }
    }
  };

  return (
    <div className="modal-overlay" onClick={handleClose}>
      <div className="modal modal-xlarge image-selection-modal" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={handleClose} disabled={loading}>
          닫기
        </button>

        <h3 className="modal-title">최종 이미지를 선택해주세요</h3>
        <p className="modal-subtitle">
          AI가 처리한 결과입니다. 원하는 이미지를 선택하고 카테고리를 확인해주세요.
        </p>

        {/* 이미지 선택 그리드 - 표준 처리 이미지 */}
        <div className="image-grid">
          {IMAGE_TYPES.map((imageType) => {
            const imageUrl = cloth[imageType.urlKey];
            if (!imageUrl) return null; // 이미지가 없으면 표시 안 함

            const isSelected = !selectedImageUrl && selectedType === imageType.type;

            return (
              <div
                key={imageType.type}
                className={`image-option ${isSelected ? 'selected' : ''}`}
                onClick={() => {
                  setSelectedType(imageType.type);
                  setSelectedImageUrl(null); // 표준 타입 선택 시 URL 초기화
                }}
              >
                <div className="image-container">
                  <img
                    src={getImageUrl(imageUrl)}
                    alt={imageType.label}
                    onError={(e) => {
                      e.target.src = '/placeholder.jpg';
                    }}
                  />
                  {isSelected && (
                    <div className="selected-badge">✓ 선택됨</div>
                  )}
                </div>
                <div className="image-label">
                  <strong>{imageType.label}</strong>
                  <span>{imageType.description}</span>
                </div>
              </div>
            );
          })}
        </div>

        {/* 추가 감지된 아이템들 */}
        {cloth.additionalItems && cloth.additionalItems.length > 0 && (
          <>
            <h4 className="section-title">추가로 감지된 아이템</h4>
            <p className="section-subtitle">
              사진에서 {cloth.additionalItems.length}개의 아이템을 더 발견했습니다. 원하는 아이템을 선택하세요.
            </p>
            <div className="image-grid">
              {cloth.additionalItems.map((item, index) => {
                const isSelected = selectedImageUrl === item.imageUrl;

                return (
                  <div
                    key={`additional-${index}`}
                    className={`image-option ${isSelected ? 'selected' : ''}`}
                    onClick={() => {
                      setSelectedImageUrl(item.imageUrl);
                      setSelectedType(null); // 추가 아이템 선택 시 타입 초기화
                    }}
                  >
                    <div className="image-container">
                      <img
                        src={getImageUrl(item.imageUrl)}
                        alt={item.label}
                        onError={(e) => {
                          e.target.src = '/placeholder.jpg';
                        }}
                      />
                      {isSelected && (
                        <div className="selected-badge">✓ 선택됨</div>
                      )}
                    </div>
                    <div className="image-label">
                      <strong>{translateLabel(item.label)}</strong>
                      <span>{item.areaPixels} pixels</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </>
        )}

        {/* 카테고리 선택 */}
        <div className="category-section">
          <label>
            <strong>카테고리</strong>
            {cloth.suggestedCategory && (
              <span className="ai-suggestion"> (AI 제안: {
                CATEGORIES.find(c => c.value === cloth.suggestedCategory)?.label || cloth.suggestedCategory
              })</span>
            )}
          </label>
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            disabled={loading}
          >
            {CATEGORIES.map((cat) => (
              <option key={cat.value} value={cat.value}>
                {cat.label}
              </option>
            ))}
          </select>
        </div>

        {/* 확인 버튼 */}
        <button
          className="btn btn-primary btn-block"
          onClick={handleConfirm}
          disabled={loading}
        >
          {loading ? '처리 중...' : '확인'}
        </button>

        <p className="hint-text">
          닫기 버튼을 클릭하면 "옷 영역 추출" 이미지가 자동으로 선택됩니다.
        </p>
      </div>
    </div>
  );
}

export default ImageSelectionModal;
