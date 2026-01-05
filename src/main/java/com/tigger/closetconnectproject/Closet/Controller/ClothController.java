package com.tigger.closetconnectproject.Closet.Controller;

import com.tigger.closetconnectproject.Closet.Dto.*;
import com.tigger.closetconnectproject.Closet.Entity.Category;
import com.tigger.closetconnectproject.Closet.Service.ClothService;
import com.tigger.closetconnectproject.Security.AppUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
     * 설명: 이미지 업로드와 함께 옷 아이템 등록 (비동기 처리)
     * - 원본 이미지 즉시 저장 후 백그라운드 파이프라인 시작
     * - 파이프라인: rembg → segmentation → inpainting
     * - 즉시 응답 반환 (PROCESSING 상태)
     * - 클라이언트는 /status 엔드포인트로 처리 상태 폴링
     *
     * @param image 업로드할 이미지 파일 (최대 10MB)
     * @param name 옷 이름
     * @param category 카테고리 (선택, AI가 제안하므로 null 가능)
     * @param principal 현재 로그인한 사용자
     * @return 생성된 옷 아이템 정보 (PROCESSING 상태)
     * @example POST /api/v1/cloth/upload
     *          Content-Type: multipart/form-data
     *          image: (파일)
     *          name: "나이키 운동화"
     *          category: (optional)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ClothResponse uploadWithImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam("name") String name,
            @RequestParam(value = "category", required = false) Category category,
            @RequestParam(value = "imageType", required = false, defaultValue = "FULL_BODY") com.tigger.closetconnectproject.Closet.Entity.ImageType imageType,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();

        // DTO 생성
        ClothUploadRequest req = new ClothUploadRequest();
        req.setName(name);
        req.setCategory(category);
        req.setImageType(imageType);

        return clothService.createWithImage(uid, image, req);
    }

    /**
     * 설명: 옷 처리 상태 조회 (폴링용)
     * - 비동기 처리 진행 상황 확인
     * - processingStatus: PROCESSING, READY_FOR_REVIEW, COMPLETED, FAILED
     * - READY_FOR_REVIEW 상태일 때 suggestedCategory 확인 가능
     *
     * @param id 조회할 옷 아이템 ID
     * @param principal 현재 로그인한 사용자
     * @return 처리 상태 정보
     * @example GET /api/v1/cloth/123/status
     */
    @GetMapping("/{id}/status")
    public ClothStatusResponse getStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        return clothService.getStatus(uid, id);
    }

    /**
     * 설명: AI 제안 카테고리 확인/수정
     * - processingStatus가 READY_FOR_REVIEW 상태일 때만 호출 가능
     * - 사용자가 카테고리를 확인하거나 수정
     * - 최종 이미지를 inpaintedImageUrl로 설정
     * - 상태를 COMPLETED로 변경
     *
     * @param id 옷 아이템 ID
     * @param req 확인된 카테고리
     * @param principal 현재 로그인한 사용자
     * @return 최종 확정된 옷 아이템 정보
     * @example PUT /api/v1/cloth/123/confirm
     *          Content-Type: application/json
     *          {"category": "TOP"}
     */
    @PutMapping("/{id}/confirm")
    public ClothResponse confirmCategory(
            @PathVariable Long id,
            @RequestBody @Valid ConfirmCategoryRequest req,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        return clothService.confirmCategory(uid, id, req);
    }

    /**
     * 설명: 최종 이미지 선택 및 옷 확정
     * - AI 처리 완료 후 사용자가 원하는 이미지를 선택
     * - 이미지 타입: ORIGINAL, REMOVED_BG, SEGMENTED, INPAINTED
     * - 카테고리도 동시에 확인/수정 가능
     * - confirmed = true로 설정되어 옷장에 표시됨
     *
     * @param id 옷 아이템 ID
     * @param req 선택한 이미지 타입 및 카테고리
     * @param principal 현재 로그인한 사용자
     * @return 확정된 옷 아이템 정보
     * @example POST /api/v1/cloth/123/confirm-image
     *          Content-Type: application/json
     *          {"selectedImageType": "INPAINTED", "category": "TOP"}
     */
    @PostMapping("/{id}/confirm-image")
    public ClothResponse confirmImage(
            @PathVariable Long id,
            @RequestBody @Valid ConfirmClothRequest req,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        return clothService.confirmCloth(uid, id, req);
    }

    /**
     * 설명: 처리 결과 거부 (NO 선택)
     * - READY_FOR_REVIEW 상태일 때 사용자가 결과를 거부하고 삭제
     * - 이미지 파일과 DB 레코드 모두 삭제
     *
     * @param id 거부할 옷 아이템 ID
     * @param principal 현재 로그인한 사용자
     * @return 204 No Content
     * @example POST /api/v1/cloth/123/reject
     */
    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        clothService.rejectCloth(uid, id);
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
