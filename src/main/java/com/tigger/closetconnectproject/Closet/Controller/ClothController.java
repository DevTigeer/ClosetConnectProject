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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 옷장 관리 API Controller
 * - SecurityConfig에서 /api/v1/cloth/** 경로는 authenticated() 설정되어 있어
 *   인증되지 않은 사용자는 접근 불가
 */
@RestController
@RequestMapping("/api/v1/cloth")
@RequiredArgsConstructor
public class ClothController {

    private final ClothService clothService;

    /**
     * 설명: 사용자의 옷 목록 조회 (페이징, 정렬, 카테고리 필터링 지원)
     * @param category 카테고리 필터 (선택, 예: TOP, BOTTOM, ACC 등)
     * @param pageable 페이징/정렬 정보 (page, size, sort)
     * @param principal 현재 로그인한 사용자 (Spring Security 자동 주입)
     * @return 옷 목록 페이지
     * @example GET /api/v1/cloth?category=ACC&page=0&size=20&sort=createdAt,desc
     */
    @GetMapping
    public Page<ClothResponse> list(
            @RequestParam(required = false) Category category,
            Pageable pageable,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        // SecurityConfig에서 인증 처리되므로 principal은 항상 not null
        Long uid = principal.getUser().getUserId();
        return clothService.list(uid, category, pageable);
    }

    /**
     * 설명: 특정 옷 아이템 단건 조회
     * - 본인 소유의 아이템만 조회 가능 (Service에서 권한 체크)
     * @param id 조회할 옷 아이템 ID
     * @param principal 현재 로그인한 사용자
     * @return 옷 아이템 상세 정보
     */
    @GetMapping("/{id}")
    public ClothResponse getOne(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        return clothService.getOne(uid, id);
    }

    /**
     * 설명: 새로운 옷 아이템 등록
     * @param req 등록할 옷 정보 (name, category, imageUrl)
     * @param principal 현재 로그인한 사용자
     * @return 생성된 옷 아이템 정보 (201 Created)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClothResponse create(
            @RequestBody @Valid ClothCreateRequest req,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        return clothService.create(uid, req);
    }

    /**
     * 설명: 옷 아이템 삭제
     * - 본인 소유의 아이템만 삭제 가능 (Service에서 권한 체크)
     * @param id 삭제할 옷 아이템 ID
     * @param principal 현재 로그인한 사용자
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        clothService.delete(uid, id);
    }
}
