import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { marketAPI, marketCommentAPI, marketChatAPI } from '../services/api';
import { getProductImageUrl, handleImageError } from '../utils/imageUtils';
import './MarketDetailPage.css';

function MarketDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const token = localStorage.getItem('accessToken');

  const [product, setProduct] = useState(null);
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [commentContent, setCommentContent] = useState('');
  const [isLiked, setIsLiked] = useState(false);

  useEffect(() => {
    fetchProductDetail();
    fetchComments();
  }, [id]);

  const fetchProductDetail = async () => {
    try {
      const response = await marketAPI.getOne(id);
      setProduct(response.data);
      setIsLiked(response.data.liked);
    } catch (error) {
      console.error('상품 상세 조회 실패:', error);
      alert('상품을 불러올 수 없습니다.');
      navigate('/market');
    } finally {
      setLoading(false);
    }
  };

  const fetchComments = async () => {
    try {
      const response = await marketCommentAPI.list(id);
      setComments(response.data || []);
    } catch (error) {
      console.error('댓글 조회 실패:', error);
    }
  };

  const handleLike = async () => {
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    try {
      if (isLiked) {
        await marketAPI.unlike(id);
        // 상태 직접 업데이트 (불필요한 API 호출 제거)
        setProduct(prev => ({
          ...prev,
          likeCount: Math.max(0, (prev.likeCount || 0) - 1)
        }));
      } else {
        await marketAPI.like(id);
        // 상태 직접 업데이트 (불필요한 API 호출 제거)
        setProduct(prev => ({
          ...prev,
          likeCount: (prev.likeCount || 0) + 1
        }));
      }
      setIsLiked(!isLiked);
    } catch (error) {
      console.error('찜하기 실패:', error);
      alert('찜하기에 실패했습니다.');
      // 에러 시 원래 상태로 되돌리기 위해 다시 가져오기
      fetchProductDetail();
    }
  };

  const handleCommentSubmit = async (e) => {
    e.preventDefault();
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    if (!commentContent.trim()) {
      alert('댓글 내용을 입력해주세요.');
      return;
    }

    try {
      await marketCommentAPI.create(id, { content: commentContent });
      setCommentContent('');
      fetchComments();
      // 댓글 개수 직접 업데이트 (불필요한 API 호출 제거)
      setProduct(prev => ({
        ...prev,
        commentCount: (prev.commentCount || 0) + 1
      }));
    } catch (error) {
      console.error('댓글 작성 실패:', error);
      alert('댓글 작성에 실패했습니다.');
    }
  };

  const handleCommentDelete = async (commentId) => {
    if (!confirm('댓글을 삭제하시겠습니까?')) return;

    try {
      await marketCommentAPI.delete(id, commentId);
      fetchComments();
      // 댓글 개수 직접 업데이트 (불필요한 API 호출 제거)
      setProduct(prev => ({
        ...prev,
        commentCount: Math.max(0, (prev.commentCount || 0) - 1)
      }));
    } catch (error) {
      console.error('댓글 삭제 실패:', error);
      alert('댓글 삭제에 실패했습니다.');
    }
  };

  const handleStartChat = async () => {
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    try {
      const response = await marketChatAPI.createRoom({ productId: parseInt(id) });
      const roomId = response.data.roomId;
      navigate(`/market/chat?roomId=${roomId}`);
    } catch (error) {
      console.error('채팅방 생성 실패:', error);
      alert('채팅방 생성에 실패했습니다.');
    }
  };

  const handleEdit = () => {
    navigate(`/market/products/${id}/edit`);
  };

  const handleDelete = async () => {
    if (!confirm('상품을 삭제하시겠습니까?')) return;

    try {
      await marketAPI.delete(id);
      alert('상품이 삭제되었습니다.');
      navigate('/market');
    } catch (error) {
      console.error('상품 삭제 실패:', error);
      alert('상품 삭제에 실패했습니다.');
    }
  };

  if (loading) {
    return (
      <div className="market-detail-page">
        <div className="page-container">
          <div className="loading-state">
            <div className="spinner"></div>
            <p>로딩 중...</p>
          </div>
        </div>
      </div>
    );
  }

  if (!product) return null;

  const isMyProduct = token && product.isMine;

  return (
    <div className="market-detail-page">
      <div className="page-container">
        {/* 뒤로가기 */}
        <button onClick={() => navigate('/market')} className="back-btn">
          ← 목록으로
        </button>

        {/* 상품 정보 */}
        <div className="product-detail-layout">
          {/* 이미지 */}
          <div className="product-image-section">
            <img
              src={getProductImageUrl(product)}
              alt={product.title}
              className="product-detail-image"
              onError={handleImageError}
            />
          </div>

          {/* 정보 */}
          <div className="product-info-section">
            <div className="product-header">
              <div>
                <span className={`status-badge ${product.status.toLowerCase()}`}>
                  {product.status === 'ON_SALE' ? '판매중' :
                   product.status === 'RESERVED' ? '예약중' : '거래완료'}
                </span>
                <h1 className="product-detail-title">{product.title}</h1>
                <p className="product-detail-price">{product.price?.toLocaleString()}원</p>
              </div>
              <button
                onClick={handleLike}
                className={`like-btn ${isLiked ? 'liked' : ''}`}
              >
                {isLiked ? '❤️' : '🤍'} {product.likeCount || 0}
              </button>
            </div>

            <div className="product-meta-info">
              <div className="meta-item">
                <span className="meta-label">지역</span>
                <span className="meta-value">{product.region}</span>
              </div>
              <div className="meta-item">
                <span className="meta-label">상태</span>
                <span className="meta-value">{product.productCondition}</span>
              </div>
              <div className="meta-item">
                <span className="meta-label">조회수</span>
                <span className="meta-value">{product.viewCount || 0}회</span>
              </div>
              <div className="meta-item">
                <span className="meta-label">판매자</span>
                <span className="meta-value">{product.sellerNickname}</span>
              </div>
            </div>

            <div className="product-description">
              <h3>상품 설명</h3>
              <p>{product.description}</p>
            </div>

            {/* 액션 버튼 */}
            <div className="product-actions">
              {isMyProduct ? (
                <>
                  <button onClick={handleEdit} className="btn-secondary">
                    수정
                  </button>
                  <button onClick={handleDelete} className="btn-danger">
                    삭제
                  </button>
                </>
              ) : (
                <button onClick={handleStartChat} className="btn-primary full-width">
                  💬 채팅하기
                </button>
              )}
            </div>
          </div>
        </div>

        {/* 댓글 섹션 */}
        <div className="comments-section">
          <h2 className="comments-title">
            댓글 <span className="comments-count">{comments.length}</span>
          </h2>

          {/* 댓글 작성 */}
          {token && (
            <form onSubmit={handleCommentSubmit} className="comment-form">
              <textarea
                value={commentContent}
                onChange={(e) => setCommentContent(e.target.value)}
                placeholder="댓글을 입력하세요"
                className="comment-textarea"
                rows="3"
              />
              <button type="submit" className="btn-primary">
                댓글 작성
              </button>
            </form>
          )}

          {/* 댓글 목록 */}
          <div className="comments-list">
            {comments.length > 0 ? (
              comments.map((comment) => (
                <div key={comment.commentId} className="comment-item">
                  <div className="comment-header">
                    <div>
                      <span className="comment-author">{comment.authorNickname}</span>
                      {comment.isSeller && (
                        <span className="seller-badge">판매자</span>
                      )}
                      <span className="comment-date">
                        {new Date(comment.createdAt).toLocaleString()}
                      </span>
                    </div>
                    {comment.isMine && (
                      <button
                        onClick={() => handleCommentDelete(comment.commentId)}
                        className="comment-delete-btn"
                      >
                        삭제
                      </button>
                    )}
                  </div>
                  <p className="comment-content">{comment.content}</p>
                </div>
              ))
            ) : (
              <div className="empty-comments">
                <p>첫 댓글을 작성해보세요!</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default MarketDetailPage;
