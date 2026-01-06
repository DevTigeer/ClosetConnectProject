import { useState } from 'react';
import { clothAPI } from '../services/api';
import { getImageUrl } from '../utils/imageUtils';
import './ImageSelectionModal.css';

const CATEGORIES = [
  { value: 'TOP', label: '상의' },
  { value: 'BOTTOM', label: '하의' },
  { value: 'SHOES', label: '신발' },
  { value: 'ACC', label: '액세서리' },
];

// 이미지 타입 옵션 생성 함수 (단일 vs 다중 옷에 따라 다름)
const getImageTypes = (isSingleItem) => {
  const baseTypes = [
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

  // 단일 옷 이미지일 때만 AI 확장 옵션 추가
  if (isSingleItem) {
    baseTypes.push({
      type: 'INPAINTED',
      label: 'AI 확장 이미지',
      description: 'Gemini로 확장/복원된 이미지',
      urlKey: 'inpaintedImageUrl'
    });
  }

  return baseTypes;
};

// AI 라벨을 한글로 변환
const translateLabel = (label) => {
  const labelMap = {
    'hat': '모자',
    'upper-clothes': '상의',
    'skirt': '치마',
    'pants': '바지',
    'dress': '원피스',
    'belt': '벨트',
    'shoes': '신발',  // 통합된 신발
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
  // 다중 아이템 레이아웃인지 판단 (allSegmentedItems가 있으면 새 레이아웃)
  const isMultiItemLayout = cloth.allSegmentedItems && cloth.allSegmentedItems.length > 0;

  // 단일 옷 이미지인지 판단 (additionalItems가 없으면 단일 옷)
  const isSingleItem = !cloth.additionalItems || cloth.additionalItems.length === 0;

  // 이미지 타입 옵션 가져오기
  const IMAGE_TYPES = getImageTypes(isSingleItem);

  // 기본 선택값: 다중 아이템이면 첫번째 확장 이미지, 단일이면 INPAINTED
  const [selectedType, setSelectedType] = useState(
    isMultiItemLayout ? null : (isSingleItem ? 'INPAINTED' : 'SEGMENTED')
  );
  const [selectedImageUrl, setSelectedImageUrl] = useState(
    isMultiItemLayout && cloth.allExpandedItems?.[0]?.expandedUrl
      ? cloth.allExpandedItems[0].expandedUrl
      : null
  );
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
    // 닫기 버튼 클릭 시 저장하지 않고 삭제 (reject)
    if (!loading) {
      const confirmed = window.confirm('저장하지 않고 취소하시겠습니까?\n등록한 옷이 삭제됩니다.');
      if (!confirmed) {
        return; // 사용자가 취소를 선택하면 아무것도 하지 않음
      }

      setLoading(true);
      try {
        await clothAPI.reject(cloth.id);

        if (onConfirm) {
          onConfirm(cloth.id);  // clothId 전달하여 activeUploads에서 제거
        }

        onClose();
      } catch (err) {
        console.error(err);
        alert('삭제에 실패했습니다.');
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

        {/* 다중 아이템 레이아웃 (새 디자인) */}
        {isMultiItemLayout ? (
          <>
            {/* 1. 원본 + 배경제거 */}
            <h4 className="section-title">기본 처리 이미지</h4>
            <div className="image-grid image-grid-2col">
              {cloth.originalImageUrl && (
                <div className="image-option-view">
                  <div className="image-container">
                    <img src={getImageUrl(cloth.originalImageUrl)} alt="원본 이미지" />
                  </div>
                  <div className="image-label">
                    <strong>원본 이미지</strong>
                  </div>
                </div>
              )}
              {cloth.removedBgImageUrl && (
                <div className="image-option-view">
                  <div className="image-container">
                    <img src={getImageUrl(cloth.removedBgImageUrl)} alt="배경 제거" />
                  </div>
                  <div className="image-label">
                    <strong>배경 제거</strong>
                  </div>
                </div>
              )}
            </div>

            {/* 2. 크롭된 아이템들 (크기순) */}
            {cloth.allSegmentedItems && cloth.allSegmentedItems.length > 0 && (
              <>
                <h4 className="section-title">크롭된 의류 파츠 ({cloth.allSegmentedItems.length}개)</h4>
                <p className="section-subtitle">픽셀 크기가 큰 순서대로 정렬되어 있습니다</p>
                <div className="image-grid">
                  {cloth.allSegmentedItems.map((item, index) => (
                    <div key={`segmented-${index}`} className="image-option-view">
                      <div className="image-container">
                        <img
                          src={getImageUrl(item.segmentedUrl)}
                          alt={translateLabel(item.label)}
                          onError={(e) => { e.target.src = '/placeholder.jpg'; }}
                        />
                      </div>
                      <div className="image-label">
                        <strong>{translateLabel(item.label)}</strong>
                        <span>{item.areaPixels?.toLocaleString()} pixels</span>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}

            {/* 3. Gemini 확장 이미지들 (크기순, 선택 가능) */}
            {cloth.allExpandedItems && cloth.allExpandedItems.length > 0 && (
              <>
                <h4 className="section-title">AI 확장 이미지 ({cloth.allExpandedItems.length}개)</h4>
                <p className="section-subtitle">
                  Gemini AI로 확장 처리된 이미지입니다. 원하는 이미지를 선택하세요.
                </p>
                <div className="image-grid">
                  {cloth.allExpandedItems.map((item, index) => {
                    const isSelected = selectedImageUrl === item.expandedUrl;

                    return (
                      <div
                        key={`expanded-${index}`}
                        className={`image-option ${isSelected ? 'selected' : ''}`}
                        onClick={() => {
                          setSelectedImageUrl(item.expandedUrl);
                          setSelectedType(null);
                        }}
                      >
                        <div className="image-container">
                          <img
                            src={getImageUrl(item.expandedUrl)}
                            alt={translateLabel(item.label)}
                            onError={(e) => { e.target.src = '/placeholder.jpg'; }}
                          />
                          {isSelected && (
                            <div className="selected-badge">✓ 선택됨</div>
                          )}
                        </div>
                        <div className="image-label">
                          <strong>{translateLabel(item.label)}</strong>
                          <span>{item.areaPixels?.toLocaleString()} pixels</span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </>
            )}
          </>
        ) : (
          /* 기존 단일 아이템 레이아웃 */
          <>
            {/* 이미지 선택 그리드 - 표준 처리 이미지 */}
            <div className="image-grid">
              {IMAGE_TYPES.map((imageType) => {
                const imageUrl = cloth[imageType.urlKey];
                if (!imageUrl) return null;

                const isSelected = !selectedImageUrl && selectedType === imageType.type;

                return (
                  <div
                    key={imageType.type}
                    className={`image-option ${isSelected ? 'selected' : ''}`}
                    onClick={() => {
                      setSelectedType(imageType.type);
                      setSelectedImageUrl(null);
                    }}
                  >
                    <div className="image-container">
                      <img
                        src={getImageUrl(imageUrl)}
                        alt={imageType.label}
                        onError={(e) => { e.target.src = '/placeholder.jpg'; }}
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

            {/* 추가 감지된 아이템들 (deprecated, 하위 호환) */}
            {cloth.additionalItems && cloth.additionalItems.length > 0 && (
              <>
                <h4 className="section-title">추가로 감지된 아이템</h4>
                <p className="section-subtitle">
                  사진에서 {cloth.additionalItems.length}개의 아이템을 더 발견했습니다.
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
                          setSelectedType(null);
                        }}
                      >
                        <div className="image-container">
                          <img
                            src={getImageUrl(item.imageUrl)}
                            alt={item.label}
                            onError={(e) => { e.target.src = '/placeholder.jpg'; }}
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
          닫기 버튼을 클릭하면 저장하지 않고 등록한 옷이 삭제됩니다.
        </p>
      </div>
    </div>
  );
}

export default ImageSelectionModal;
