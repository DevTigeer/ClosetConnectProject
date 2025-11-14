import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { postAPI, boardAPI } from '../services/api';
import CommentSection from '../components/CommentSection';
import './PostDetailPage.css';

function PostDetailPage() {
  const { slug, postId } = useParams();
  const navigate = useNavigate();

  const [post, setPost] = useState(null);
  const [board, setBoard] = useState(null);
  const [likes, setLikes] = useState({ count: 0, liked: false });
  const [loading, setLoading] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({ title: '', content: '' });

  // 게시글 조회
  const fetchPost = async () => {
    setLoading(true);
    try {
      const response = await postAPI.getOne(postId);
      setPost(response.data);
      setEditForm({
        title: response.data.title,
        content: response.data.content,
      });
    } catch (err) {
      console.error(err);
      alert('게시글을 불러오는데 실패했습니다.');
      navigate(-1);
    } finally {
      setLoading(false);
    }
  };

  // 좋아요 정보 조회
  const fetchLikes = async () => {
    try {
      const response = await postAPI.getLikes(postId);
      setLikes(response.data);
    } catch (err) {
      console.error(err);
    }
  };

  // 보드 정보 조회
  const fetchBoard = async () => {
    try {
      const response = await boardAPI.getBySlug(slug);
      setBoard(response.data);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    fetchBoard();
    fetchPost();
    fetchLikes();
  }, [postId, slug]);

  // 좋아요 토글
  const handleLikeToggle = async () => {
    try {
      if (likes.liked) {
        await postAPI.unlike(postId);
      } else {
        await postAPI.like(postId);
      }
      fetchLikes();
    } catch (err) {
      console.error(err);
      if (err.response?.status === 401) {
        alert('로그인이 필요합니다.');
      } else {
        alert('좋아요 처리에 실패했습니다.');
      }
    }
  };

  // 게시글 수정
  const handleEdit = async (e) => {
    e.preventDefault();
    try {
      await postAPI.update(postId, editForm);
      setIsEditing(false);
      fetchPost();
      alert('게시글이 수정되었습니다.');
    } catch (err) {
      console.error(err);
      alert('수정에 실패했습니다.');
    }
  };

  // 게시글 삭제
  const handleDelete = async () => {
    if (!confirm('정말 이 게시글을 삭제하시겠습니까?')) {
      return;
    }

    try {
      await postAPI.delete(postId);
      alert('게시글이 삭제되었습니다.');
      navigate(`/community/${slug}`);
    } catch (err) {
      console.error(err);
      alert('삭제에 실패했습니다.');
    }
  };

  if (loading || !post) {
    return <div className="loading">로딩 중...</div>;
  }

  const isAuthor = post.isAuthor; // 백엔드에서 제공하는 필드
  const createdDate = post.createdAt ? post.createdAt.split('T')[0] : '';

  return (
    <div className="post-detail-page">
      <header className="post-detail-header">
        <Link to={`/community/${slug}`} className="back-link">
          ← 목록으로
        </Link>
      </header>

      <div className="post-detail-content">
        {!isEditing ? (
          <>
            <div className="post-header">
              <h1 className="post-title">{post.title}</h1>
              <div className="post-meta">
                <span className="author">{post.authorNickname}</span>
                <span className="separator">·</span>
                <span className="date">{createdDate}</span>
                <span className="separator">·</span>
                <span className="views">조회 {post.viewCount || 0}</span>
              </div>
            </div>

            <div className="post-body">
              <p className="post-content">{post.content}</p>
            </div>

            <div className="post-actions">
              <button
                onClick={handleLikeToggle}
                className={`like-btn ${likes.liked ? 'liked' : ''}`}
              >
                ❤️ {likes.count}
              </button>

              {isAuthor && (
                <div className="author-actions">
                  <button onClick={() => setIsEditing(true)} className="btn btn-edit">
                    수정
                  </button>
                  <button onClick={handleDelete} className="btn btn-delete">
                    삭제
                  </button>
                </div>
              )}
            </div>
          </>
        ) : (
          <form onSubmit={handleEdit} className="edit-form">
            <div className="form-field">
              <label>제목</label>
              <input
                type="text"
                value={editForm.title}
                onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                required
              />
            </div>

            <div className="form-field">
              <label>내용</label>
              <textarea
                value={editForm.content}
                onChange={(e) => setEditForm({ ...editForm, content: e.target.value })}
                rows="10"
                required
              />
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary">
                저장
              </button>
              <button
                type="button"
                onClick={() => setIsEditing(false)}
                className="btn"
              >
                취소
              </button>
            </div>
          </form>
        )}

        <CommentSection postId={postId} isAnonymous={board?.type === 'ANONYMOUS'} />
      </div>
    </div>
  );
}

export default PostDetailPage;
