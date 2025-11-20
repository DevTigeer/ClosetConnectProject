import { getClothImageUrl, handleImageError } from '../utils/imageUtils';
import './ClothCard.css';

function ClothCard({ cloth, deleteMode, isSelected, onToggleSelect, onClick, onSell }) {
  const imageUrl = getClothImageUrl(cloth);

  const handleClick = (e) => {
    if (deleteMode) {
      e.stopPropagation();
      onToggleSelect();
    } else {
      onClick();
    }
  };

  const handleSell = (e) => {
    e.stopPropagation();
    onSell();
  };

  return (
    <div className={`cloth-card ${deleteMode ? 'delete-mode' : ''} ${isSelected ? 'selected' : ''}`} onClick={handleClick}>
      {deleteMode && (
        <input
          type="checkbox"
          className="cloth-checkbox"
          checked={isSelected}
          onChange={onToggleSelect}
          onClick={(e) => e.stopPropagation()}
        />
      )}

      <div className="cloth-image-wrapper">
        <img
          src={imageUrl}
          alt={cloth.name}
          className="cloth-image"
          onError={handleImageError}
        />
        {!deleteMode && (
          <div className="cloth-overlay">
            <button onClick={handleSell} className="cloth-sell-btn">
              🛍️ 중고거래 등록
            </button>
          </div>
        )}
      </div>

      <div className="cloth-meta">
        <span className="cloth-name">{cloth.name}</span>
        <span className="cloth-category">{cloth.category}</span>
      </div>
    </div>
  );
}

export default ClothCard;
