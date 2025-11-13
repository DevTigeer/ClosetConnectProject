import './ClothCard.css';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

function ClothCard({ cloth, deleteMode, isSelected, onToggleSelect, onClick }) {
  const imageUrl = cloth.imageUrl?.startsWith('http')
    ? cloth.imageUrl
    : `${API_BASE}${cloth.imageUrl}`;

  const handleClick = (e) => {
    if (deleteMode) {
      e.stopPropagation();
      onToggleSelect();
    } else {
      onClick();
    }
  };

  return (
    <div className={`cloth-card ${deleteMode ? 'delete-mode' : ''}`} onClick={handleClick}>
      {deleteMode && (
        <input
          type="checkbox"
          className="cloth-checkbox"
          checked={isSelected}
          onChange={onToggleSelect}
          onClick={(e) => e.stopPropagation()}
        />
      )}

      <img src={imageUrl} alt={cloth.name} className="cloth-image" />

      <div className="cloth-meta">
        <span className="cloth-name">{cloth.name}</span>
        <span className="cloth-category">{cloth.category}</span>
      </div>
    </div>
  );
}

export default ClothCard;
