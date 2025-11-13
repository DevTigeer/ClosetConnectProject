package com.tigger.closetconnectproject.Post.Repository;

import com.tigger.closetconnectproject.Post.Entity.Post;
import com.tigger.closetconnectproject.Post.Entity.PostStatus;
import com.tigger.closetconnectproject.Post.Entity.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
        SELECT p FROM Post p
        WHERE p.board.id = :boardId
          AND p.status IN :statuses
          AND (:q IS NULL OR p.title LIKE %:q% OR p.content LIKE %:q%)
        """)
    Page<Post> searchByBoard(@Param("boardId") Long boardId,
                             @Param("statuses") List<PostStatus> statuses,
                             @Param("q") String q,
                             Pageable pageable);

    @EntityGraph(attributePaths = {"attachments", "author"})
    Optional<Post> findById(Long id);

    @Query("""
        select p
        from Post p
        left join fetch p.author a
        where p.id = :id
    """)
    Optional<Post> findByIdWithAuthor(@Param("id") Long id);

    long countByBoard_IdAndVisibilityAndStatus(Long boardId, Visibility visibility, PostStatus status);
}
