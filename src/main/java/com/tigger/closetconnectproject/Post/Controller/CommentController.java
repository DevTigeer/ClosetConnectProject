package com.tigger.closetconnectproject.Post.Controller;

import com.tigger.closetconnectproject.Security.AppUserDetails;
import com.tigger.closetconnectproject.Post.Dto.CommentDtos;
import com.tigger.closetconnectproject.Post.Service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public Page<CommentDtos.CommentRes> list(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return commentService.list(postId, page, size);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public CommentDtos.CommentRes create(
            @PathVariable Long postId,
            @RequestBody CommentDtos.CreateReq req,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        return commentService.create(postId, principal.getUser().getUserId(), req);
    }

    @PatchMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public CommentDtos.CommentRes update(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentDtos.UpdateReq req,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return commentService.update(commentId, principal.getUser().getUserId(), req, isAdmin);
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public void delete(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        commentService.delete(commentId, principal.getUser().getUserId(), isAdmin);
    }
}
