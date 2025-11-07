package com.tigger.closetconnectproject.Post.Repository;

import com.tigger.closetconnectproject.Post.Entity.Comment;
import com.tigger.closetconnectproject.Post.Entity.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByPost_IdAndStatusOrderByCreatedAtAsc(Long postId, CommentStatus status, Pageable pageable);
}
