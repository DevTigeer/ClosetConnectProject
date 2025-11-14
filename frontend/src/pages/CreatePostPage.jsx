import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { postAPI, boardAPI } from '../services/api';
import './CreatePostPage.css';

function CreatePostPage() {
  const { slug } = useParams();
  const navigate = useNavigate();

  const [board, setBoard] = useState(null);
  const [formData, setFormData] = useState({
    title: '',
    content: '',
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchBoard = async () => {
      try {
        const response = await boardAPI.getBySlug(slug);
        setBoard(response.data);
      } catch (err) {
        console.error(err);
        alert('보드 정보를 불러오는데 실패했습니다.');
        navigate(-1);
      }
    };

    fetchBoard();
  }, [slug]);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.title.trim() || !formData.content.trim()) {
      alert('제목과 내용을 모두 입력해주세요.');
      return;
    }

    setLoading(true);
    try {
      const response = await postAPI.create(board.id, formData);
      alert('게시글이 작성되었습니다.');
      navigate(`/community/${slug}/posts/${response.data.id}`);
    } catch (err) {
      console.error(err);
      if (err.response?.status === 401) {
        alert('로그인이 필요합니다.');
        navigate('/login');
      } else {
        alert('게시글 작성에 실패했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (!board) {
    return <div className="loading">로딩 중...</div>;
  }

  return (
    <div className="create-post-page">
      <header className="create-post-header">
        <Link to={`/community/${slug}`} className="back-link">
          ← 취소
        </Link>
        <h1>게시글 작성</h1>
      </header>

      <div className="create-post-content">
        <div className="board-info">
          <span className="board-label">게시판</span>
          <span className="board-name">{board.name}</span>
        </div>

        <form onSubmit={handleSubmit} className="post-form">
          <div className="form-field">
            <label htmlFor="title">제목</label>
            <input
              id="title"
              type="text"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              placeholder="제목을 입력하세요"
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="content">내용</label>
            <textarea
              id="content"
              value={formData.content}
              onChange={(e) => setFormData({ ...formData, content: e.target.value })}
              placeholder="내용을 입력하세요"
              rows="15"
              required
            />
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? '작성 중...' : '작성하기'}
            </button>
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="btn"
              disabled={loading}
            >
              취소
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default CreatePostPage;
