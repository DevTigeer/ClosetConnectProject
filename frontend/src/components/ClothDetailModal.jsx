import { getClothImageUrl } from '../utils/imageUtils';
import './Modal.css';

function ClothDetailModal({ cloth, onClose }) {
  const imageUrl = getClothImageUrl(cloth);

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
