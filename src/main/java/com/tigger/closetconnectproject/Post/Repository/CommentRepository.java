package com.tigger.closetconnectproject.Post.Repository;

import com.tigger.closetconnectproject.Post.Entity.Comment;
import com.tigger.closetconnectproject.Post.Entity.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 게시글의 댓글 목록 조회 (N+1 방지: 작성자 정보 함께 로딩)
     */
    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByPost_IdAndStatusOrderByCreatedAtAsc(Long postId, CommentStatus status, Pageable pageable);
}
