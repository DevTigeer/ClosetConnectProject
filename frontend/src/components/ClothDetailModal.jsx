import './Modal.css';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

function ClothDetailModal({ cloth, onClose }) {
  const imageUrl = cloth.imageUrl?.startsWith('http')
    ? cloth.imageUrl
    : `${API_BASE}${cloth.imageUrl}`;

  const createdDate = cloth.createdAt ? cloth.createdAt.split('T')[0] : '';

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>
          닫기
        </button>

        <img src={imageUrl} alt={cloth.name} className="modal-image" />

        <h3 className="modal-title">{cloth.name}</h3>

        <div className="modal-info">
          <div className="info-row">
            <span className="info-label">카테고리:</span>
            <span className="info-value">{cloth.category}</span>
          </div>
          <div className="info-row">
            <span className="info-label">등록일:</span>
            <span className="info-value">{createdDate}</span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ClothDetailModal;
