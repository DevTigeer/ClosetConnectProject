package com.tigger.closetconnectproject.Post.Controller;

import com.tigger.closetconnectproject.Security.AppUserDetails;
import com.tigger.closetconnectproject.Post.Dto.PostDtos;
import com.tigger.closetconnectproject.Post.Service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostReadController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final PostService postService;

    /**
     * 사용자가 관리자 권한을 가지고 있는지 확인
     */
    private boolean isAdmin(AppUserDetails principal) {
        return principal.getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals(ROLE_ADMIN));
    }

    @GetMapping("/{postId}")
    public PostDtos.PostRes read(
            @PathVariable Long postId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = (principal != null) ? principal.getUser().getUserId() : null;
        postService.increaseView(postId);
        return postService.read(postId, uid);
    }

    @PatchMapping("/{postId}")
    @PreAuthorize("isAuthenticated()")
    public PostDtos.PostRes update(
            @PathVariable Long postId,
            @RequestBody PostDtos.UpdateReq req,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        return postService.update(postId, uid, req, isAdmin(principal));
    }

    @DeleteMapping("/{postId}")
    @PreAuthorize("isAuthenticated()")
    public void delete(
            @PathVariable Long postId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        postService.delete(postId, uid, isAdmin(principal));
    }

    @PostMapping("/{postId}/like")
    @PreAuthorize("isAuthenticated()")
    public void like(
            @PathVariable Long postId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        postService.like(postId, principal.getUser().getUserId());
    }

    @DeleteMapping("/{postId}/like")
    @PreAuthorize("isAuthenticated()")
    public void unlike(
            @PathVariable Long postId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        postService.unlike(postId, principal.getUser().getUserId());
    }

    @PostMapping(value = "/{postId}/attachments", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public PostDtos.AttachmentRes attach(
            @PathVariable Long postId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AppUserDetails principal
    ) throws Exception {
        return postService.uploadAttachment(postId, principal.getUser().getUserId(), file);
    }
    @GetMapping("/{postId}/likes")
    public java.util.Map<String, Object> getLikes(
            @PathVariable Long postId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = (principal != null) ? principal.getUser().getUserId() : null;
        long count = postService.likeCount(postId);
        boolean liked = postService.likedBy(postId, uid);
        return java.util.Map.of("count", count, "liked", liked);
    }
    @GetMapping("/{postId}/likes/count")
    public java.util.Map<String, Object> getLikeCount(@PathVariable Long postId) {
        long count = postService.likeCount(postId);
        return java.util.Map.of("count", count);
    }
}
