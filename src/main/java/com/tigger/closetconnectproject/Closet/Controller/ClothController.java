package com.tigger.closetconnectproject.Closet.Controller;

import com.tigger.closetconnectproject.Closet.Dto.ClothCreateRequest;
import com.tigger.closetconnectproject.Closet.Dto.ClothResponse;
import com.tigger.closetconnectproject.Closet.Entity.Category;
import com.tigger.closetconnectproject.Closet.Service.ClothService;
import com.tigger.closetconnectproject.Security.AppUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cloth")
@RequiredArgsConstructor
public class ClothController {

    private final ClothService clothService;

    /** 목록 조회: /api/v1/cloth?category=ACC&page=0&size=20&sort=createdAt,desc */
    @GetMapping
    public Page<ClothResponse> list(
            @RequestParam(required = false) Category category,
            Pageable pageable,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        if (principal == null) throw new BadCredentialsException("인증이 필요합니다.");
        Long uid = principal.getUser().getUserId();
        return clothService.list(uid, category, pageable);
    }

    /** 단건 조회 */
    @GetMapping("/{id}")
    public ClothResponse getOne(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        if (principal == null) throw new BadCredentialsException("인증이 필요합니다.");
        Long uid = principal.getUser().getUserId();
        return clothService.getOne(uid, id);
    }

    /** 생성 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClothResponse create(
            @RequestBody @Valid ClothCreateRequest req,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        if (principal == null) throw new BadCredentialsException("인증이 필요합니다.");
        Long uid = principal.getUser().getUserId();
        return clothService.create(uid, req);
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        if (principal == null) throw new BadCredentialsException("인증이 필요합니다.");
        Long uid = principal.getUser().getUserId();
        clothService.delete(uid, id);
    }
}
