package com.tigger.closetconnectproject.Upload.Controller;

import com.tigger.closetconnectproject.Security.AppUserDetails;
import com.tigger.closetconnectproject.Upload.Dto.UploadResponse;
import com.tigger.closetconnectproject.Upload.Service.LocalStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final LocalStorageService storage;

    /** 이미지 업로드 전용 엔드포인트 */
    @PostMapping(consumes = "multipart/form-data")
    public UploadResponse upload(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AppUserDetails principal
    ) throws Exception {
        if (principal == null) throw new BadCredentialsException("인증이 필요합니다.");
        Long uid = principal.getUser().getUserId();

        var saved = storage.store(file, uid);
        return new UploadResponse(saved.imageUrl(), saved.imageKey());
    }
}
