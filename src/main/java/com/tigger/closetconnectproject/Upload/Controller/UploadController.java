package com.tigger.closetconnectproject.Upload.Controller;

import com.tigger.closetconnectproject.Security.AppUserDetails;
import com.tigger.closetconnectproject.Upload.Dto.UploadResponse;
import com.tigger.closetconnectproject.Upload.Service.LocalStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 업로드 API Controller
 * - 인증된 사용자만 파일 업로드 가능
 */
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final LocalStorageService storage;

    /**
     * 설명: 이미지 파일 업로드
     * - 허용 형식: JPG, PNG, WEBP
     * - 저장 위치: {uploadRoot}/{userId}/{year}/{month}/{day}/{uuid}.ext
     * - 반환: 공개 URL 및 내부 imageKey
     * @param file 업로드할 이미지 파일 (multipart/form-data)
     * @param principal 현재 로그인한 사용자 (Spring Security 자동 주입)
     * @return 업로드된 파일의 URL과 Key 정보
     * @throws Exception 파일 저장 실패 시
     */
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()") // 명시적으로 인증 필요 표시
    public UploadResponse upload(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AppUserDetails principal
    ) throws Exception {
        // @PreAuthorize로 인증 체크되므로 principal은 항상 not null
        Long uid = principal.getUser().getUserId();

        // 파일 저장 (LocalStorageService에서 MIME 타입 검증 및 저장)
        var saved = storage.store(file, uid);
        return new UploadResponse(saved.imageUrl(), saved.imageKey());
    }
}
