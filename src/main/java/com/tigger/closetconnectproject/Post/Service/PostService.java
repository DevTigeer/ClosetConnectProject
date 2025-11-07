package com.tigger.closetconnectproject.Post.Service;

import com.tigger.closetconnectproject.Community.Entity.CommunityBoard;
import com.tigger.closetconnectproject.Post.Dto.PostDtos;
import com.tigger.closetconnectproject.Post.Entity.*;
import com.tigger.closetconnectproject.Post.Repository.*;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.Upload.Service.LocalStorageService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepo;
    private final PostLikeRepository likeRepo;
    private final PostAttachmentRepository attRepo;
    private final com.tigger.closetconnectproject.Community.Repository.CommunityBoardRepository boardRepo;
    private final LocalStorageService storage;

    public Page<PostDtos.PostRes> list(Long boardId, int page, int size, String sort, String q, Long viewerId) {
        Sort s = switch (sort == null ? "LATEST" : sort.toUpperCase()) {
            case "LIKE" -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "VIEW" -> Sort.by(Sort.Direction.DESC, "viewCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.min(size,100), s);
        var statuses = List.of(PostStatus.NORMAL, PostStatus.HIDDEN);
        Page<Post> data = postRepo.searchByBoard(boardId, statuses, (q == null || q.isBlank()) ? null : q, pageable);

        return data.map(p -> {
            boolean liked = viewerId != null && likeRepo.existsByPost_IdAndUser_UserId(p.getId(), viewerId);
            var atts = p.getAttachments().stream().map(a ->
                    PostDtos.AttachmentRes.builder()
                            .id(a.getId()).url(a.getUrl()).filename(a.getFilename())
                            .contentType(a.getContentType()).size(a.getSize()).build()
            ).toList();
            return PostDtos.PostRes.of(p, liked, atts);
        });
    }

    public PostDtos.PostRes create(Long boardId, Long authorUserId, PostDtos.CreateReq req) {
        CommunityBoard board = boardRepo.getReferenceById(boardId);
        Users authorRef = Users.builder().userId(authorUserId).build(); // getReference 대용

        Post p = Post.builder()
                .board(board).author(authorRef)
                .title(req.getTitle()).content(req.getContent())
                .visibility(req.getVisibility())
                .status(PostStatus.NORMAL)
                .pinned(false).viewCount(0).likeCount(0)
                .build();
        postRepo.save(p);

        return PostDtos.PostRes.of(p, false, List.of());
    }

    @Transactional(readOnly = true)
    public PostDtos.PostRes read(Long postId, Long viewerId) {
        Post p = postRepo.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        boolean liked = viewerId != null && likeRepo.existsByPost_IdAndUser_UserId(postId, viewerId);
        var atts = p.getAttachments().stream().map(a ->
                PostDtos.AttachmentRes.builder()
                        .id(a.getId()).url(a.getUrl()).filename(a.getFilename())
                        .contentType(a.getContentType()).size(a.getSize()).build()
        ).toList();
        return PostDtos.PostRes.of(p, liked, atts);
    }

    public void increaseView(Long postId) {
        Post p = postRepo.findById(postId).orElseThrow();
        p.increaseView();
    }

    public PostDtos.AttachmentRes uploadAttachment(Long postId, Long userId, MultipartFile file) throws Exception {
        Post p = postRepo.findById(postId).orElseThrow();
        if (!p.getAuthor().getUserId().equals(userId))
            throw new AccessDeniedException("작성자만 첨부 가능");

        // 1) 실제 스토리지 저장 (URL/KEY 획득)
        var saved = storage.store(file, userId); // returns Stored(imageKey, imageUrl)

        // 2) 파일 메타데이터는 요청으로부터 추출
        String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("uploaded");
        String contentType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");
        long size = file.getSize();

        // 3) 엔티티 저장
        var att = attRepo.save(PostAttachment.builder()
                .post(p)
                .url(saved.imageUrl())
                .imageKey(saved.imageKey())
                .filename(originalFilename)
                .contentType(contentType)
                .size(size)
                .build());

        // 4) 응답 DTO 매핑
        return PostDtos.AttachmentRes.builder()
                .id(att.getId())
                .url(att.getUrl())
                .filename(att.getFilename())
                .contentType(att.getContentType())
                .size(att.getSize())
                .build();
    }


    public void like(Long postId, Long userId) {
        if (likeRepo.existsByPost_IdAndUser_UserId(postId, userId)) return;
        Post p = postRepo.findById(postId).orElseThrow();
        likeRepo.save(PostLike.builder()
                .post(p).user(Users.builder().userId(userId).build()).build());
        p.incLike();
    }

    public void unlike(Long postId, Long userId) {
        likeRepo.deleteByPost_IdAndUser_UserId(postId, userId);
        Post p = postRepo.findById(postId).orElseThrow();
        p.decLike();
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
}
