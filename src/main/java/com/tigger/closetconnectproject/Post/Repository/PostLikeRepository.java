package com.tigger.closetconnectproject.Post.Repository;

import com.tigger.closetconnectproject.Post.Entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByPost_IdAndUser_UserId(Long postId, Long userId);
    void deleteByPost_IdAndUser_UserId(Long postId, Long userId);
}
