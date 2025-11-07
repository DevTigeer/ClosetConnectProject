package com.tigger.closetconnectproject.Post.Controller;

import com.tigger.closetconnectproject.Post.Dto.AdminPostDtos;
import com.tigger.closetconnectproject.Post.Dto.PostDtos;
import com.tigger.closetconnectproject.Post.Entity.PostStatus;
import com.tigger.closetconnectproject.Post.Service.PostAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

    private final PostAdminService adminService;

    /** 관리자 목록: 특정 보드에서 상태 필터로 전체 조회 */
    @GetMapping("/boards/{boardId}/posts")
    public Page<PostDtos.PostRes> list(
            @PathVariable Long boardId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(required = false, name = "status") String[] statusParams
    ) {
        // status=NORMAL&status=HIDDEN ... 형태 지원. 없으면 전체 상태.
        List<PostStatus> statuses = (statusParams == null || statusParams.length == 0)
                ? Arrays.asList(PostStatus.values())
                : Arrays.stream(statusParams).map(s -> PostStatus.valueOf(s.toUpperCase())).toList();

        return adminService.listForAdmin(boardId, statuses, q, page, size, sort);
    }

    /** 상태 변경(NORMAL/HIDDEN/BLINDED/DELETED) */
    @PatchMapping("/posts/{postId}/status")
    public void updateStatus(@PathVariable Long postId,
                             @RequestBody AdminPostDtos.UpdateStatusReq req) {
        adminService.updateStatus(postId, req.getStatus());
        // req.reason 은 감사로그/통계에 활용하고 싶다면 AOP나 별도 테이블로 남기면 됨.
    }

    /** 핀 고정/해제 */
    @PatchMapping("/posts/{postId}/pin")
    public void pin(@PathVariable Long postId,
                    @RequestBody AdminPostDtos.PinReq req) {
        adminService.pin(postId, req.isPinned());
    }

    /** 보드 이동 */
    @PatchMapping("/posts/{postId}/move")
    public void move(@PathVariable Long postId,
                     @RequestBody AdminPostDtos.MoveReq req) {
        adminService.move(postId, req.getToBoardId());
    }

    /** 하드 삭제(주의: 복구 불가) */
    @DeleteMapping("/posts/{postId}/hard")
    public void hardDelete(@PathVariable Long postId) {
        adminService.hardDelete(postId);
    }
}
