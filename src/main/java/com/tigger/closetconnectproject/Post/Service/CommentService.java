package com.tigger.closetconnectproject.Post.Service;

import com.tigger.closetconnectproject.Post.Dto.CommentDtos;
import com.tigger.closetconnectproject.Post.Entity.*;
import com.tigger.closetconnectproject.Post.Repository.CommentRepository;
import com.tigger.closetconnectproject.Post.Repository.PostRepository;
import com.tigger.closetconnectproject.User.Entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepo;
    private final PostRepository postRepo;

    public Page<CommentDtos.CommentRes> list(Long postId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.min(size,100), Sort.by("createdAt").ascending());
        return commentRepo.findByPost_IdAndStatusOrderByCreatedAtAsc(postId, CommentStatus.NORMAL, pageable)
                .map(CommentDtos.CommentRes::of);
    }

    public CommentDtos.CommentRes create(Long postId, Long authorUserId, CommentDtos.CreateReq req) {
        Post post = postRepo.findById(postId).orElseThrow();
        Comment parent = null;
        if (req.getParentId() != null) {
            parent = commentRepo.findById(req.getParentId()).orElseThrow();
            if (!parent.getPost().getId().equals(postId)) {
                throw new IllegalArgumentException("부모 댓글이 다른 게시글에 속합니다.");
            }
        }
        Comment c = Comment.builder()
                .post(post)
                .author(Users.builder().userId(authorUserId).build())
                .parent(parent)
                .content(req.getContent())
                .status(CommentStatus.NORMAL)
                .build();
        commentRepo.save(c);
        return CommentDtos.CommentRes.of(c);
    }

    public CommentDtos.CommentRes update(Long commentId, Long editorUserId, CommentDtos.UpdateReq req, boolean isAdmin) {
        Comment c = commentRepo.findById(commentId).orElseThrow();
        boolean owner = c.getAuthor() != null && c.getAuthor().getUserId().equals(editorUserId);
        if (!owner && !isAdmin) throw new AccessDeniedException("권한 없음");
        c.edit(req.getContent());
        return CommentDtos.CommentRes.of(c);
    }

    public void delete(Long commentId, Long userId, boolean isAdmin) {
        Comment c = commentRepo.findById(commentId).orElseThrow();
        boolean owner = c.getAuthor() != null && c.getAuthor().getUserId().equals(userId);
        if (!owner && !isAdmin) throw new AccessDeniedException("권한 없음");
        c.softDelete();
    }
}
