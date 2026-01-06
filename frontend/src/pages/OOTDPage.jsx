import { useState, useEffect } from 'react';
import { ootdAPI } from '../services/api';
import './OOTDPage.css';

function OOTDPage() {
  const [ootds, setOotds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedImage, setSelectedImage] = useState(null);

  useEffect(() => {
    fetchOotds();
  }, []);

  const fetchOotds = async () => {
    try {
      setLoading(true);
      const response = await ootdAPI.list();
      setOotds(response.data);
    } catch (err) {
      console.error('OOTD 목록 조회 실패:', err);
      setError('OOTD 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('정말로 이 OOTD를 삭제하시겠습니까?')) {
      return;
    }

    try {
      await ootdAPI.delete(id);
      setOotds(ootds.filter((ootd) => ootd.id !== id));
    } catch (err) {
      console.error('OOTD 삭제 실패:', err);
      alert('OOTD 삭제에 실패했습니다.');
    }
  };

  const handleDownload = async (imageUrl, id) => {
    try {
      const fullImageUrl = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}${imageUrl}`;
      const response = await fetch(fullImageUrl);
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `ootd-${id}-${Date.now()}.png`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('다운로드 실패:', err);
      alert('이미지 다운로드에 실패했습니다.');
    }
  };

  if (loading) {
    return (
      <div className="ootd-page">
        <div className="ootd-header">
          <h1>✨ My OOTD</h1>
          <p className="ootd-subtitle">나만의 코디 컬렉션</p>
        </div>
        <div className="loading">로딩 중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="ootd-page">
        <div className="ootd-header">
          <h1>✨ My OOTD</h1>
          <p className="ootd-subtitle">나만의 코디 컬렉션</p>
        </div>
        <div className="error-message">{error}</div>
      </div>
    );
  }

  return (
    <div className="ootd-page">
      <div className="ootd-header">
        <h1>✨ My OOTD</h1>
        <p className="ootd-subtitle">나만의 코디 컬렉션 ({ootds.length}개)</p>
      </div>

      {ootds.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">👔</div>
          <h3>저장된 OOTD가 없습니다</h3>
          <p>조합하기로 만든 코디를 저장해보세요!</p>
        </div>
      ) : (
        <div className="ootd-grid">
          {ootds.map((ootd) => (
            <div key={ootd.id} className="ootd-card">
              <div className="ootd-image-wrapper" onClick={() => setSelectedImage(ootd)}>
                <img
                  src={`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}${ootd.imageUrl}`}
                  alt={ootd.description || 'OOTD'}
                  className="ootd-image"
                />
                <div className="ootd-overlay">
                  <span className="zoom-icon">🔍</span>
                </div>
              </div>
              {ootd.description && <p className="ootd-description">{ootd.description}</p>}
              <div className="ootd-date">
                {new Date(ootd.createdAt).toLocaleDateString('ko-KR', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                })}
              </div>
              <div className="ootd-actions">
                <button
                  className="btn-download-small"
                  onClick={() => handleDownload(ootd.imageUrl, ootd.id)}
                  title="다운로드"
                >
                  <span>⬇️</span>
                </button>
                <button
                  className="btn-delete-small"
                  onClick={() => handleDelete(ootd.id)}
                  title="삭제"
                >
                  <span>🗑️</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 이미지 확대 모달 */}
      {selectedImage && (
        <div className="image-modal-overlay" onClick={() => setSelectedImage(null)}>
          <div className="image-modal-content" onClick={(e) => e.stopPropagation()}>
            <button className="modal-close-btn" onClick={() => setSelectedImage(null)}>
              ×
            </button>
            <img
              src={`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}${selectedImage.imageUrl}`}
              alt={selectedImage.description || 'OOTD'}
              className="modal-image"
            />
            {selectedImage.description && (
              <p className="modal-description">{selectedImage.description}</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default OOTDPage;
