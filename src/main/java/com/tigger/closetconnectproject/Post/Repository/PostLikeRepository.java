// src/main/java/com/tigger/closetconnectproject/Post/Repository/PostLikeRepository.java
package com.tigger.closetconnectproject.Post.Repository;

import com.tigger.closetconnectproject.Post.Entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    // ✅ 연관경로를 정확히 표기 (Post.id, Users.userId)
    boolean existsByPost_IdAndUser_UserId(Long postId, Long userId);

    long countByPost_Id(Long postId);

    void deleteByPost_IdAndUser_UserId(Long postId, Long userId);

    // ✅ MariaDB/MySQL용: INSERT IGNORE (PostgreSQL의 ON CONFLICT DO NOTHING 대체)
    @Modifying
    @Query(value = "INSERT IGNORE INTO community_post_like(post_id, user_id) VALUES (?1, ?2)", nativeQuery = true)
    void insert(Long postId, Long userId);

    // ✅ 파생쿼리로 충분 (JPQL 수동작성 불필요)
    List<PostLike> findAllByPost_IdInAndUser_UserId(List<Long> postIds, Long userId);
}
