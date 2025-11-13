import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { boardAPI, postAPI } from '../services/api';
import './BoardPage.css';

function BoardPage() {
  const { slug } = useParams();
  const [board, setBoard] = useState(null);
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    const fetchBoard = async () => {
      try {
        const response = await boardAPI.getBySlug(slug);
        setBoard(response.data);
      } catch (err) {
        console.error(err);
        alert('보드를 불러오는데 실패했습니다.');
      }
    };

    fetchBoard();
  }, [slug]);

  useEffect(() => {
    if (!board) return;

    const fetchPosts = async () => {
      setLoading(true);
      try {
        const response = await postAPI.list(board.id, {
          page,
          size: 20,
          sort: 'LATEST',
        });
        setPosts(response.data.content || []);
        setTotalPages(response.data.totalPages || 0);
      } catch (err) {
        console.error(err);
        alert('게시글을 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchPosts();
  }, [board, page]);

  if (!board) {
    return <div className="loading">로딩 중...</div>;
  }

  return (
    <div className="board-page">
      <header className="board-header">
        <div>
          <Link to="/community" className="back-link">
            ← 커뮤니티
          </Link>
          <h1>{board.name}</h1>
        </div>
      </header>

      {loading ? (
        <div className="loading">로딩 중...</div>
      ) : (
        <div className="post-list">
          {posts.length === 0 ? (
            <div className="empty-state">
              <p>작성된 게시글이 없습니다.</p>
            </div>
          ) : (
            <>
              {posts.map((post) => (
                <div key={post.id} className="post-item">
                  <div className="post-title">{post.title}</div>
                  <div className="post-meta">
                    {post.authorNickname} · {post.createdAt?.split('T')[0]}
                  </div>
                </div>
              ))}

              {totalPages > 1 && (
                <div className="pagination">
                  <button
                    onClick={() => setPage(page - 1)}
                    disabled={page === 0}
                    className="btn"
                  >
                    이전
                  </button>
                  <span className="page-info">
                    {page + 1} / {totalPages}
                  </span>
                  <button
                    onClick={() => setPage(page + 1)}
                    disabled={page >= totalPages - 1}
                    className="btn"
                  >
                    다음
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}

export default BoardPage;
