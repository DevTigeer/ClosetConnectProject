package com.tigger.closetconnectproject.Post.Controller;

import com.tigger.closetconnectproject.Common.Security.AppUserDetails;
import com.tigger.closetconnectproject.Post.Dto.PostDtos;
import com.tigger.closetconnectproject.Post.Service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/boards/{boardId}/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public Page<PostDtos.PostRes> list(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long viewerId = (principal != null) ? principal.getUser().getUserId() : null;
        return postService.list(boardId, page, size, sort, q, viewerId);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public PostDtos.PostRes create(
            @PathVariable Long boardId,
            @RequestBody PostDtos.CreateReq req,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        return postService.create(boardId, uid, req);
    }
}
