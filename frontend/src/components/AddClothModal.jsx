import { useState } from 'react';
import { clothAPI } from '../services/api';
import { getImageUrl } from '../utils/imageUtils';
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
  const [formData, setFormData] = useState({
    name: '',
    category: 'TOP',
  });
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState('');
  const [loading, setLoading] = useState(false);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // 파일 저장 및 미리보기 생성
    setSelectedFile(file);
    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!selectedFile) {
      alert('이미지를 먼저 선택해주세요.');
      return;
    }

    setLoading(true);
    try {
      // 이미지 업로드와 함께 옷 등록 (rembg 배경 제거 포함)
      const response = await clothAPI.uploadWithImage(
        selectedFile,
        formData.name,
        formData.category
      );

      // 성공 시 부모 컴포넌트에 알림
      if (onSubmit) {
        await onSubmit(response.data);
      }

      onClose();
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.message || '등록에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
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
