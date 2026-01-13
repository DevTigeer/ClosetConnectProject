import { useState, useEffect } from 'react';
import { clothAPI } from '../services/api';
import { getImageUrl } from '../utils/imageUtils';
import { useClothUpload } from '../contexts/ClothUploadContext';
import './Modal.css';

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

function AddClothModal({ onClose, onSubmit }) {
  const { addUpload } = useClothUpload();

  const [formData, setFormData] = useState({
    name: '',
    category: 'TOP',
    imageType: 'FULL_BODY', // 'FULL_BODY' or 'SINGLE_ITEM'
  });
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [isOptimizing, setIsOptimizing] = useState(false);
  const [optimizationInfo, setOptimizationInfo] = useState(null);

  /**
   * 이미지 리사이즈 함수
   * AI 처리 속도 향상을 위해 이미지를 최대 1200px로 리사이즈하고 JPEG로 압축
   */
  const resizeImage = (file, maxSize = 1200, quality = 0.9) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();

      reader.onerror = () => reject(new Error('파일을 읽을 수 없습니다.'));

      reader.onload = (e) => {
        const img = new Image();

        img.onerror = () => reject(new Error('이미지를 로드할 수 없습니다.'));

        img.onload = () => {
          const canvas = document.createElement('canvas');
          let width = img.width;
          let height = img.height;

          // 최대 크기 제한 (비율 유지)
          if (width > maxSize || height > maxSize) {
            if (width > height) {
              height = (height / width) * maxSize;
              width = maxSize;
            } else {
              width = (width / height) * maxSize;
              height = maxSize;
            }
          }

          canvas.width = width;
          canvas.height = height;

          const ctx = canvas.getContext('2d');
          ctx.drawImage(img, 0, 0, width, height);

          // JPEG로 변환하여 파일 크기 감소
          canvas.toBlob(
            (blob) => {
              if (blob) {
                resolve({
                  blob,
                  width,
                  height,
                  originalSize: file.size,
                  optimizedSize: blob.size,
                });
              } else {
                reject(new Error('이미지 변환에 실패했습니다.'));
              }
            },
            'image/jpeg',
            quality
          );
        };

        img.src = e.target.result;
      };

      reader.readAsDataURL(file);
    });
  };

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // 이전 미리보기 URL 정리 (메모리 누수 방지)
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }

    // 이미지 최적화 시작
    setIsOptimizing(true);
    setOptimizationInfo(null);

    try {
      // 이미지 최적화 (리사이즈 + JPEG 압축)
      const result = await resizeImage(file, 1200, 0.9);

      // 최적화된 이미지를 File 객체로 변환
      const optimizedFile = new File(
        [result.blob],
        file.name.replace(/\.[^/.]+$/, '.jpg'), // 확장자를 .jpg로 변경
        { type: 'image/jpeg' }
      );

      // 파일 저장 및 미리보기 생성
      setSelectedFile(optimizedFile);
      const objectUrl = URL.createObjectURL(result.blob);
      setPreviewUrl(objectUrl);

      // 최적화 정보 저장
      const reductionPercent = ((1 - result.optimizedSize / result.originalSize) * 100).toFixed(0);
      setOptimizationInfo({
        originalSize: (result.originalSize / 1024).toFixed(0),
        optimizedSize: (result.optimizedSize / 1024).toFixed(0),
        reduction: reductionPercent,
        dimensions: `${result.width}×${result.height}`,
      });

      console.log(`이미지 최적화 완료: ${(result.originalSize / 1024).toFixed(0)}KB → ${(result.optimizedSize / 1024).toFixed(0)}KB (${reductionPercent}% 감소)`);
    } catch (error) {
      console.error('이미지 최적화 실패:', error);

      // 최적화 실패 시 원본 파일 사용
      setSelectedFile(file);
      const objectUrl = URL.createObjectURL(file);
      setPreviewUrl(objectUrl);

      setOptimizationInfo({
        error: true,
        message: '최적화 실패 - 원본 이미지 사용',
      });
    } finally {
      setIsOptimizing(false);
    }
  };

  // 컴포넌트 unmount 시 미리보기 URL 정리
  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!selectedFile) {
      alert('이미지를 먼저 선택해주세요.');
      return;
    }

    setLoading(true);
    try {
      // 이미지 업로드와 함께 옷 등록
      const response = await clothAPI.uploadWithImage(
        selectedFile,
        formData.name,
        formData.category,
        formData.imageType
      );

      const clothId = response.data.id;

      // 응답에서 실제 userId 가져오기 (ClothResponse에 직접 포함됨)
      const userId = response.data.userId;

      // 전역 진행도 트래커에 추가
      addUpload(clothId, userId);

      // 성공 알림
      alert('옷이 등록되었습니다! AI 처리가 백그라운드로 진행됩니다.');

      // 모달 즉시 닫기 (사용자는 다른 작업 가능)
      onClose();

      // 부모 컴포넌트에 알림 (옷장 목록 새로고침 등)
      if (onSubmit) {
        onSubmit(response.data);
      }
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.message || '등록에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };


  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal modal-large" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>
          닫기
        </button>

        <h3 className="modal-title">옷 추가</h3>

        <form onSubmit={handleSubmit}>
          <div className="form-field">
            <label>이미지 파일</label>
            <input
              type="file"
              accept="image/*"
              onChange={handleFileChange}
              disabled={loading}
            />

            {/* 이미지 최적화 진행 중 표시 */}
            {isOptimizing && (
              <div className="text-sm text-gray-500 mt-2">
                🔄 이미지 최적화 중... (AI 처리 속도가 빨라집니다)
              </div>
            )}

            {/* 최적화 완료 정보 표시 */}
            {optimizationInfo && !optimizationInfo.error && (
              <div className="text-sm text-green-600 mt-2">
                ✅ 이미지 최적화 완료: {optimizationInfo.originalSize}KB → {optimizationInfo.optimizedSize}KB
                ({optimizationInfo.reduction}% 감소, {optimizationInfo.dimensions})
              </div>
            )}

            {/* 최적화 실패 시 경고 표시 */}
            {optimizationInfo && optimizationInfo.error && (
              <div className="text-sm text-yellow-600 mt-2">
                ⚠️ {optimizationInfo.message}
              </div>
            )}

            {previewUrl && (
              <img src={previewUrl} alt="Preview" className="image-preview" />
            )}
          </div>

          <div className="form-field">
            <label>이름</label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
            />
          </div>

          <div className="form-field">
            <label>이미지 타입</label>
            <div className="radio-group">
              <label className="radio-label">
                <input
                  type="radio"
                  name="imageType"
                  value="FULL_BODY"
                  checked={formData.imageType === 'FULL_BODY'}
                  onChange={(e) => setFormData({ ...formData, imageType: e.target.value })}
                  disabled={loading}
                />
                <span>전신 사진 (사람 이미지)</span>
                <small>사람이 옷을 입고 있는 사진</small>
              </label>
              <label className="radio-label">
                <input
                  type="radio"
                  name="imageType"
                  value="SINGLE_ITEM"
                  checked={formData.imageType === 'SINGLE_ITEM'}
                  onChange={(e) => setFormData({ ...formData, imageType: e.target.value })}
                  disabled={loading}
                />
                <span>단일 옷 이미지</span>
                <small>옷만 촬영한 사진 (상의, 하의 등)</small>
              </label>
            </div>
          </div>

          <div className="form-field">
            <label>카테고리</label>
            <select
              value={formData.category}
              onChange={(e) => setFormData({ ...formData, category: e.target.value })}
              required
            >
              {CATEGORIES.map((cat) => (
                <option key={cat.value} value={cat.value}>
                  {cat.label}
                </option>
              ))}
            </select>
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
            {loading ? '등록 중...' : '등록'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default AddClothModal;
