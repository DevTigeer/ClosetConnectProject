import { useState, useEffect } from 'react';
import { adminBoardAPI, adminPostAPI, boardAPI } from '../services/api';
import './AdminPage.css';

function AdminPage() {
  const [activeTab, setActiveTab] = useState('boards');
  const [boards, setBoards] = useState([]);
  const [posts, setPosts] = useState([]);
  const [selectedBoard, setSelectedBoard] = useState(null);
  const [loading, setLoading] = useState(false);

  // 보드 생성 폼
  const [showBoardForm, setShowBoardForm] = useState(false);
  const [boardForm, setBoardForm] = useState({
    name: '',
    slug: '',
    type: 'GENERAL',
    visibility: 'PUBLIC',
    system: false,
    sortOrder: 0,
  });

  useEffect(() => {
    if (activeTab === 'boards') {
      fetchBoards();
    }
  }, [activeTab]);

  useEffect(() => {
    if (selectedBoard) {
      fetchPosts();
    }
  }, [selectedBoard]);

  // 보드 목록 조회
  const fetchBoards = async () => {
    setLoading(true);
    try {
      const response = await adminBoardAPI.listAll();
      setBoards(response.data || []);
    } catch (err) {
      console.error(err);
      alert('보드 목록 조회 실패');
    } finally {
      setLoading(false);
    }
  };

  // 게시글 목록 조회
  const fetchPosts = async () => {
    setLoading(true);
    try {
      const response = await adminPostAPI.list(selectedBoard.id, {
        page: 0,
        size: 50,
      });
      setPosts(response.data.content || []);
    } catch (err) {
      console.error(err);
      alert('게시글 목록 조회 실패');
    } finally {
      setLoading(false);
    }
  };

  // 보드 생성
  const handleCreateBoard = async (e) => {
    e.preventDefault();
    try {
      await adminBoardAPI.create(boardForm);
      setShowBoardForm(false);
      setBoardForm({
        name: '',
        slug: '',
        type: 'GENERAL',
        visibility: 'PUBLIC',
        system: false,
        sortOrder: 0,
      });
      fetchBoards();
      alert('보드가 생성되었습니다.');
    } catch (err) {
      console.error(err);
      alert('보드 생성 실패');
    }
  };

  // 보드 삭제
  const handleDeleteBoard = async (id) => {
    if (!confirm('보드를 삭제하시겠습니까?')) return;
    try {
      await adminBoardAPI.delete(id);
      fetchBoards();
      alert('보드가 삭제되었습니다.');
    } catch (err) {
      console.error(err);
      alert('보드 삭제 실패');
    }
  };

  // 공개범위 변경
  const handleChangeVisibility = async (id, visibility) => {
    try {
      await adminBoardAPI.changeVisibility(id, visibility);
      fetchBoards();
      alert('공개범위가 변경되었습니다.');
    } catch (err) {
      console.error(err);
      alert('공개범위 변경 실패');
    }
  };

  // 게시글 상태 변경
  const handleChangePostStatus = async (postId, status) => {
    try {
      await adminPostAPI.updateStatus(postId, status);
      fetchPosts();
      alert('게시글 상태가 변경되었습니다.');
    } catch (err) {
      console.error(err);
      alert('상태 변경 실패');
    }
  };

  // 게시글 핀 고정/해제
  const handleTogglePin = async (postId, currentPinned) => {
    try {
      await adminPostAPI.pin(postId, !currentPinned);
      fetchPosts();
    } catch (err) {
      console.error(err);
      alert('핀 고정 실패');
    }
  };

  // 게시글 하드 삭제
  const handleHardDelete = async (postId) => {
    if (!confirm('게시글을 영구 삭제하시겠습니까? 복구할 수 없습니다.')) return;
    try {
      await adminPostAPI.hardDelete(postId);
      fetchPosts();
      alert('게시글이 삭제되었습니다.');
    } catch (err) {
      console.error(err);
      alert('삭제 실패');
    }
  };

  return (
    <div className="admin-page">
      <header className="admin-header">
        <h1>⚙️ 관리자 페이지</h1>
      </header>

      <div className="admin-tabs">
        <button
          className={`tab ${activeTab === 'boards' ? 'active' : ''}`}
          onClick={() => setActiveTab('boards')}
        >
          보드 관리
        </button>
        <button
          className={`tab ${activeTab === 'posts' ? 'active' : ''}`}
          onClick={() => setActiveTab('posts')}
        >
          게시글 관리
        </button>
      </div>

      <div className="admin-content">
        {activeTab === 'boards' && (
          <div className="board-management">
            <div className="section-header">
              <h2>보드 목록</h2>
              <button
                className="btn btn-primary"
                onClick={() => setShowBoardForm(!showBoardForm)}
              >
                {showBoardForm ? '취소' : '+ 보드 생성'}
              </button>
            </div>

            {showBoardForm && (
              <form onSubmit={handleCreateBoard} className="board-form">
                <div className="form-row">
                  <div className="form-field">
                    <label>보드 이름</label>
                    <input
                      type="text"
                      value={boardForm.name}
                      onChange={(e) =>
                        setBoardForm({ ...boardForm, name: e.target.value })
                      }
                      required
                    />
                  </div>
                  <div className="form-field">
                    <label>슬러그</label>
                    <input
                      type="text"
                      value={boardForm.slug}
                      onChange={(e) =>
                        setBoardForm({ ...boardForm, slug: e.target.value })
                      }
                      required
                    />
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-field">
                    <label>타입</label>
                    <select
                      value={boardForm.type}
                      onChange={(e) =>
                        setBoardForm({ ...boardForm, type: e.target.value })
                      }
                    >
                      <option value="GENERAL">일반</option>
                      <option value="ANONYMOUS">익명</option>
                      <option value="NOTICE">공지</option>
                    </select>
                  </div>
                  <div className="form-field">
                    <label>공개범위</label>
                    <select
                      value={boardForm.visibility}
                      onChange={(e) =>
                        setBoardForm({ ...boardForm, visibility: e.target.value })
                      }
                    >
                      <option value="PUBLIC">공개</option>
                      <option value="PRIVATE">비공개</option>
                      <option value="HIDDEN">숨김</option>
                    </select>
                  </div>
                </div>

                <button type="submit" className="btn btn-primary">
                  생성
                </button>
              </form>
            )}

            {loading ? (
              <div className="loading">로딩 중...</div>
            ) : (
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>이름</th>
                    <th>슬러그</th>
                    <th>타입</th>
                    <th>공개범위</th>
                    <th>작업</th>
                  </tr>
                </thead>
                <tbody>
                  {boards.map((board) => (
                    <tr key={board.id}>
                      <td>{board.id}</td>
                      <td>{board.name}</td>
                      <td>{board.slug}</td>
                      <td>{board.type}</td>
                      <td>
                        <select
                          value={board.visibility}
                          onChange={(e) =>
                            handleChangeVisibility(board.id, e.target.value)
                          }
                          className="inline-select"
                        >
                          <option value="PUBLIC">공개</option>
                          <option value="PRIVATE">비공개</option>
                          <option value="HIDDEN">숨김</option>
                        </select>
                      </td>
                      <td>
                        <button
                          onClick={() => handleDeleteBoard(board.id)}
                          className="btn-small btn-danger"
                        >
                          삭제
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

        {activeTab === 'posts' && (
          <div className="post-management">
            <div className="section-header">
              <h2>게시글 관리</h2>
              <select
                value={selectedBoard?.id || ''}
                onChange={(e) => {
                  const board = boards.find((b) => b.id === Number(e.target.value));
                  setSelectedBoard(board);
                }}
                className="board-select"
              >
                <option value="">보드 선택</option>
                {boards.map((board) => (
                  <option key={board.id} value={board.id}>
                    {board.name}
                  </option>
                ))}
              </select>
            </div>

            {selectedBoard ? (
              loading ? (
                <div className="loading">로딩 중...</div>
              ) : posts.length === 0 ? (
                <div className="empty-state">게시글이 없습니다.</div>
              ) : (
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>제목</th>
                      <th>작성자</th>
                      <th>상태</th>
                      <th>핀</th>
                      <th>작업</th>
                    </tr>
                  </thead>
                  <tbody>
                    {posts.map((post) => (
                      <tr key={post.id}>
                        <td>{post.id}</td>
                        <td className="post-title-cell">{post.title}</td>
                        <td>{post.authorNickname}</td>
                        <td>
                          <select
                            value={post.status}
                            onChange={(e) =>
                              handleChangePostStatus(post.id, e.target.value)
                            }
                            className="inline-select"
                          >
                            <option value="NORMAL">정상</option>
                            <option value="HIDDEN">숨김</option>
                            <option value="BLINDED">블라인드</option>
                            <option value="DELETED">삭제</option>
                          </select>
                        </td>
                        <td>
                          <button
                            onClick={() => handleTogglePin(post.id, post.pinned)}
                            className={`btn-small ${
                              post.pinned ? 'btn-pinned' : ''
                            }`}
                          >
                            {post.pinned ? '📌' : '📍'}
                          </button>
                        </td>
                        <td>
                          <button
                            onClick={() => handleHardDelete(post.id)}
                            className="btn-small btn-danger"
                          >
                            영구삭제
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )
            ) : (
              <div className="empty-state">
                보드를 선택해주세요.
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default AdminPage;
