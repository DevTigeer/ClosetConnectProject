// src/main/java/com/tigger/closetconnectproject/Post/Service/PostService.java
package com.tigger.closetconnectproject.Post.Service;

import com.tigger.closetconnectproject.Community.Entity.CommunityBoard;
import com.tigger.closetconnectproject.Post.Dto.PostDtos;
import com.tigger.closetconnectproject.Post.Entity.Post;
import com.tigger.closetconnectproject.Post.Entity.PostAttachment;
import com.tigger.closetconnectproject.Post.Entity.PostLike;
import com.tigger.closetconnectproject.Post.Entity.PostStatus;
import com.tigger.closetconnectproject.Post.Entity.Visibility;
import com.tigger.closetconnectproject.Post.Repository.PostAttachmentRepository;
import com.tigger.closetconnectproject.Post.Repository.PostLikeRepository;
import com.tigger.closetconnectproject.Post.Repository.PostRepository;
import com.tigger.closetconnectproject.Upload.Service.LocalStorageService;
import com.tigger.closetconnectproject.User.Entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepo;
    private final PostLikeRepository likeRepo;
    private final PostAttachmentRepository attRepo;
    private final com.tigger.closetconnectproject.Community.Repository.CommunityBoardRepository boardRepo;
    private final LocalStorageService storage;


    // src/main/java/com/tigger/closetconnectproject/Post/Service/PostService.java
    @Transactional(readOnly = true)
    public PostDtos.PostRes read(Long postId, Long viewerId) {
        var p = postRepo.findByIdWithAuthor(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        boolean liked = viewerId != null && likeRepo.existsByPost_IdAndUser_UserId(postId, viewerId);

        var atts = p.getAttachments().stream()
                .map(a -> PostDtos.AttachmentRes.builder()
                        .id(a.getId())
                        .url(a.getUrl())
                        .filename(a.getFilename())
                        .contentType(a.getContentType())
                        .size(a.getSize())
                        .build())
                .toList();

        // ✅ authorName은 PostRes.of 내부에서 (닉네임 null이면 '익명') 처리됨
        return PostDtos.PostRes.of(p, liked, atts);
    }

    @Transactional(readOnly = true)
    public Page<PostDtos.PostRes> list(Long boardId, int page, int size, String sort, String q, Long viewerId) {
        Sort s = switch ((sort == null ? "LATEST" : sort).toUpperCase()) {
            case "LIKE" -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "VIEW" -> Sort.by(Sort.Direction.DESC, "viewCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 100), s);

        var statuses = java.util.List.of(PostStatus.NORMAL, PostStatus.HIDDEN);
        Page<Post> data = postRepo.searchByBoard(
                boardId,
                statuses,
                (q == null || q.isBlank()) ? null : q,
                pageable
        );

        // 좋아요 여부 일괄 조회(멱등)
        java.util.Set<Long> likedIds;
        if (viewerId != null && !data.isEmpty()) {
            var ids = data.getContent().stream().map(Post::getId).toList();
            likedIds = likeRepo.findAllByPost_IdInAndUser_UserId(ids, viewerId)
                    .stream().map(pl -> pl.getPost().getId())
                    .collect(java.util.stream.Collectors.toSet());
        } else {
            likedIds = Collections.emptySet();
        }

        return data.map(p -> {
            boolean liked = viewerId != null && likedIds.contains(p.getId());
            var atts = p.getAttachments().stream()
                    .map(a -> PostDtos.AttachmentRes.builder()
                            .id(a.getId())
                            .url(a.getUrl())
                            .filename(a.getFilename())
                            .contentType(a.getContentType())
                            .size(a.getSize())
                            .build())
                    .toList();
            // ✅ 여기서도 동일하게 3개 인자만 전달
            return PostDtos.PostRes.of(p, liked, atts);
        });
    }


    public void increaseView(Long postId) {
        Post p = postRepo.findById(postId).orElseThrow();
        p.increaseView();
    }

    public PostDtos.PostRes create(Long boardId, Long authorUserId, PostDtos.CreateReq req) {
        CommunityBoard board = boardRepo.getReferenceById(boardId);
        Users authorRef = Users.builder().userId(authorUserId).build();

        Post p = Post.builder()
                .board(board)
                .author(authorRef)
                .title(req.getTitle())
                .content(req.getContent())
                .visibility(Optional.ofNullable(req.getVisibility()).orElse(Visibility.PUBLIC))
                .status(PostStatus.NORMAL)
                .pinned(false)
                .viewCount(0)
                .likeCount(0)
                .build();
        postRepo.save(p);

        return PostDtos.PostRes.of(p, false, List.of());
    }

    public PostDtos.PostRes update(Long postId, Long editorUserId, PostDtos.UpdateReq req, boolean isAdmin) {
        Post p = postRepo.findById(postId).orElseThrow();
        boolean owner = p.getAuthor() != null && p.getAuthor().getUserId().equals(editorUserId);
        if (!owner && !isAdmin) throw new AccessDeniedException("권한 없음");

        p.edit(req.getTitle(), req.getContent(), req.getVisibility());
        return read(p.getId(), editorUserId);
    }

    public void delete(Long postId, Long userId, boolean isAdmin) {
        Post p = postRepo.findById(postId).orElseThrow();
        boolean owner = p.getAuthor() != null && p.getAuthor().getUserId().equals(userId);
        if (!owner && !isAdmin) throw new AccessDeniedException("권한 없음");
        p.softDelete();
    }

    public PostDtos.AttachmentRes uploadAttachment(Long postId, Long userId, MultipartFile file) throws Exception {
        Post p = postRepo.findById(postId).orElseThrow();
        if (p.getAuthor() == null || !p.getAuthor().getUserId().equals(userId))
            throw new AccessDeniedException("작성자만 첨부 가능");

        var saved = storage.store(file, userId); // imageKey(), imageUrl()

        PostAttachment att = attRepo.save(PostAttachment.builder()
                .post(p)
                .url(saved.imageUrl())
                .imageKey(saved.imageKey())
                .filename(Optional.ofNullable(file.getOriginalFilename()).orElse("uploaded"))
                .contentType(Optional.ofNullable(file.getContentType()).orElse("application/octet-stream"))
                .size(file.getSize())
                .build());

        return PostDtos.AttachmentRes.builder()
                .id(att.getId())
                .url(att.getUrl())
                .filename(att.getFilename())
                .contentType(att.getContentType())
                .size(att.getSize())
                .build();
    }

    // 좋아요(멱등) — 연관경로 메서드로 일치
    public void like(Long postId, Long userId) {
        // 이미 있으면 무시
        if (likeRepo.existsByPost_IdAndUser_UserId(postId, userId)) return;
        // FK 체크
        postRepo.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        // MariaDB: INSERT IGNORE
        likeRepo.insert(postId, userId);

        // (선택) 엔티티 집계 필드 쓰는 경우
        // Post p = postRepo.getReferenceById(postId);
        // p.incLike();
    }

    public void unlike(Long postId, Long userId) {
        // 게시글 존재 확인(옵션)
        postRepo.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        likeRepo.deleteByPost_IdAndUser_UserId(postId, userId);

        // (선택) 집계 필드 사용하는 경우
        // Post p = postRepo.getReferenceById(postId);
        // p.decLike();
    }
    // src/main/java/com/tigger/closetconnectproject/Post/Service/PostService.java
    @Transactional(readOnly = true)
    public long likeCount(Long postId) {
        return likeRepo.countByPost_Id(postId);
    }

    @Transactional(readOnly = true)
    public boolean likedBy(Long postId, Long userId) {
        return userId != null && likeRepo.existsByPost_IdAndUser_UserId(postId, userId);
    }


}
