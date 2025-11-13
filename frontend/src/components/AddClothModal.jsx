import { useState } from 'react';
import { uploadAPI } from '../services/api';
import './Modal.css';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

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
    imageUrl: '',
  });
  const [previewUrl, setPreviewUrl] = useState('');
  const [uploading, setUploading] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setUploading(true);
    try {
      const response = await uploadAPI.upload(file);
      const imageUrl = response.data.imageUrl;

      setFormData({ ...formData, imageUrl });
      setPreviewUrl(`${API_BASE}${imageUrl}`);
    } catch (err) {
      console.error(err);
      alert('이미지 업로드에 실패했습니다.');
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.imageUrl) {
      alert('이미지를 먼저 업로드해주세요.');
      return;
    }

    setLoading(true);
    try {
      await onSubmit(formData);
    } catch (err) {
      console.error(err);
      alert('등록에 실패했습니다.');
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
              disabled={uploading}
            />
            {uploading && <p className="upload-status">업로드 중...</p>}
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
