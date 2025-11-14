import { useState, useEffect } from 'react';
import { commentAPI } from '../services/api';
import './CommentSection.css';

function CommentSection({ postId }) {
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [commentText, setCommentText] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [editText, setEditText] = useState('');

  const fetchComments = async () => {
    setLoading(true);
    try {
      const response = await commentAPI.list(postId, { page: 0, size: 100 });
      setComments(response.data.content || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchComments();
  }, [postId]);

  // 댓글 작성
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!commentText.trim()) return;

    try {
      await commentAPI.create(postId, { content: commentText });
      setCommentText('');
      fetchComments();
    } catch (err) {
      console.error(err);
      if (err.response?.status === 401) {
        alert('로그인이 필요합니다.');
      } else {
        alert('댓글 작성에 실패했습니다.');
      }
    }
  };

  // 댓글 수정
  const handleEdit = async (commentId) => {
    if (!editText.trim()) return;

    try {
      await commentAPI.update(postId, commentId, { content: editText });
      setEditingId(null);
      setEditText('');
      fetchComments();
    } catch (err) {
      console.error(err);
      alert('댓글 수정에 실패했습니다.');
    }
  };

  // 댓글 삭제
  const handleDelete = async (commentId) => {
    if (!confirm('댓글을 삭제하시겠습니까?')) return;

    try {
      await commentAPI.delete(postId, commentId);
      fetchComments();
    } catch (err) {
      console.error(err);
      alert('댓글 삭제에 실패했습니다.');
    }
  };

  // 수정 모드 시작
  const startEdit = (comment) => {
    setEditingId(comment.id);
    setEditText(comment.content);
  };

  // 수정 취소
  const cancelEdit = () => {
    setEditingId(null);
    setEditText('');
  };

  return (
    <div className="comment-section">
      <h3 className="comment-title">댓글 {comments.length}</h3>

      {/* 댓글 작성 폼 */}
      <form onSubmit={handleSubmit} className="comment-form">
        <textarea
          value={commentText}
          onChange={(e) => setCommentText(e.target.value)}
          placeholder="댓글을 입력하세요..."
          rows="3"
        />
        <button type="submit" className="btn btn-primary">
          댓글 작성
        </button>
      </form>

      {/* 댓글 목록 */}
      {loading ? (
        <div className="loading">댓글 로딩 중...</div>
      ) : comments.length === 0 ? (
        <div className="empty-comments">첫 댓글을 작성해보세요!</div>
      ) : (
        <div className="comment-list">
          {comments.map((comment) => (
            <div key={comment.id} className="comment-item">
              <div className="comment-header">
                <span className="comment-author">{comment.authorNickname}</span>
                <span className="comment-date">
                  {comment.createdAt ? comment.createdAt.split('T')[0] : ''}
                </span>
              </div>

              {editingId === comment.id ? (
                <div className="comment-edit">
                  <textarea
                    value={editText}
                    onChange={(e) => setEditText(e.target.value)}
                    rows="3"
                  />
                  <div className="comment-edit-actions">
                    <button
                      onClick={() => handleEdit(comment.id)}
                      className="btn btn-sm btn-primary"
                    >
                      저장
                    </button>
                    <button onClick={cancelEdit} className="btn btn-sm">
                      취소
                    </button>
                  </div>
                </div>
              ) : (
                <>
                  <p className="comment-content">{comment.content}</p>
                  {comment.isAuthor && (
                    <div className="comment-actions">
                      <button
                        onClick={() => startEdit(comment)}
                        className="comment-action-btn"
                      >
                        수정
                      </button>
                      <button
                        onClick={() => handleDelete(comment.id)}
                        className="comment-action-btn delete"
                      >
                        삭제
                      </button>
                    </div>
                  )}
                </>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default CommentSection;
