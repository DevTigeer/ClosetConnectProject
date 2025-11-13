package com.tigger.closetconnectproject.Post.Service;

import com.tigger.closetconnectproject.Community.Entity.CommunityBoard;
import com.tigger.closetconnectproject.Post.Dto.PostDtos;
import com.tigger.closetconnectproject.Post.Entity.Post;
import com.tigger.closetconnectproject.Post.Entity.PostStatus;
import com.tigger.closetconnectproject.Post.Repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostAdminService {

    private final PostRepository postRepo;
    private final com.tigger.closetconnectproject.Community.Repository.CommunityBoardRepository boardRepo;

    /** 관리자 목록: 숨김/블라인드/삭제 포함해 전체 검색 */
    @Transactional(readOnly = true)
    public Page<PostDtos.PostRes> listForAdmin(Long boardId,
                                               List<PostStatus> statuses,
                                               String q, int page, int size, String sort) {
        Sort s = switch (sort == null ? "LATEST" : sort.toUpperCase()) {
            case "LIKE" -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "VIEW" -> Sort.by(Sort.Direction.DESC, "viewCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        Pageable pageable = PageRequest.of(Math.max(0,page), Math.min(200,size), s);

        var data = postRepo.searchByBoard(boardId, statuses, (q == null || q.isBlank()) ? null : q, pageable);

        return data.map(p -> PostDtos.PostRes.of(p, false,
                p.getAttachments().stream().map(a ->
                        PostDtos.AttachmentRes.builder()
                                .id(a.getId()).url(a.getUrl()).filename(a.getFilename())
                                .contentType(a.getContentType()).size(a.getSize()).build()
                ).toList()
        ));
    }

    public void updateStatus(Long postId, PostStatus status) {
        Post p = postRepo.findById(postId).orElseThrow();
        switch (status) {
            case NORMAL -> p.restore();
            case HIDDEN -> p.hide();
            case BLINDED -> p.blind();
            case DELETED -> p.softDelete(); // 운영정책: 기본은 소프트 삭제
        }
    }

    public void pin(Long postId, boolean pinned) {
        Post p = postRepo.findById(postId).orElseThrow();
        if (pinned) p.pin(); else p.unpin();
    }

    public void move(Long postId, Long toBoardId) {
        Post p = postRepo.findById(postId).orElseThrow();
        CommunityBoard to = boardRepo.getReferenceById(toBoardId);
        p.moveToBoard(to);
    }

    /** 하드 삭제가 필요하면 별도 엔드포인트로 */
    public void hardDelete(Long postId) {
        postRepo.deleteById(postId);
    }
}
