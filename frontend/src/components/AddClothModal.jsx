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

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // 이전 미리보기 URL 정리 (메모리 누수 방지)
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }

    // 파일 저장 및 미리보기 생성
    setSelectedFile(file);
    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);
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
