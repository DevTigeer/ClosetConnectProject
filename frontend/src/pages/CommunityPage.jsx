import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { boardAPI } from '../services/api';
import './CommunityPage.css';

function CommunityPage() {
  const [boards, setBoards] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchBoards = async () => {
      setLoading(true);
      try {
        const response = await boardAPI.listPublic();
        setBoards(response.data || []);
      } catch (err) {
        console.error(err);
        alert('보드 목록을 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchBoards();
  }, []);

  return (
    <div className="community-page">
      <header className="community-header">
        <h1>커뮤니티</h1>
      </header>

      {loading ? (
        <div className="loading">로딩 중...</div>
      ) : (
        <div className="board-grid">
          {boards.length === 0 ? (
            <div className="empty-state">
              <p>표시할 보드가 없습니다.</p>
            </div>
          ) : (
            boards.map((board) => (
              <Link
                key={board.id}
                to={`/community/${board.slug}`}
                className="board-card"
              >
                <div className="board-name">{board.name}</div>
                <div className="board-meta">
                  {board.slug} · {board.type} · {board.visibility}
                </div>
              </Link>
            ))
          )}
        </div>
      )}
    </div>
  );
}

export default CommunityPage;
