package com.tigger.closetconnectproject.Post.Service;

import com.tigger.closetconnectproject.Post.Entity.Post;
import com.tigger.closetconnectproject.Post.Repository.PostLikeRepository;
import com.tigger.closetconnectproject.Post.Repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    /** 좋아요 */
    @Transactional
    public void like(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("post not found: " + postId));

        // ✅ 이미 누른 경우 무시 (멱등)
        boolean exists = postLikeRepository.existsByPost_IdAndUser_UserId(postId, userId);
        if (exists) return;

        // ✅ MariaDB용 INSERT IGNORE 사용 (레포지토리에 구현됨)
        postLikeRepository.insert(postId, userId);

        // ✅ Post 엔티티에 likeCount 필드가 있다면 증감 처리
        post.incLike();
    }

    /** 좋아요 취소 */
    @Transactional
    public void unlike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("post not found: " + postId));

        // ✅ 없으면 무시 (멱등)
        if (!postLikeRepository.existsByPost_IdAndUser_UserId(postId, userId)) return;

        postLikeRepository.deleteByPost_IdAndUser_UserId(postId, userId);

        post.decLike();
    }

    /** 현재 좋아요 수 반환 */
    @Transactional(readOnly = true)
    public long count(Long postId) {
        return postLikeRepository.countByPost_Id(postId);
    }
}
