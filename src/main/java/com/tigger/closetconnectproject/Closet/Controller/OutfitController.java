package com.tigger.closetconnectproject.Closet.Controller;

import com.tigger.closetconnectproject.Closet.Dto.OutfitDtos;
import com.tigger.closetconnectproject.Closet.Service.OutfitTryonService;
import com.tigger.closetconnectproject.Security.AppUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Outfit Try-On REST API 컨트롤러
 * 의류 조합 및 가상 착용 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/outfit")
@RequiredArgsConstructor
public class OutfitController {

    private final OutfitTryonService outfitTryonService;

    /**
     * Outfit Try-On 생성
     * POST /api/v1/outfit/tryon
     *
     * @param userDetails 인증된 사용자 정보
     * @param request Try-On 요청 DTO
     * @return Try-On 응답 DTO
     */
    @PostMapping("/tryon")
    public ResponseEntity<OutfitDtos.TryonResponse> createTryon(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody OutfitDtos.CreateTryonRequest request
    ) {
        log.info("POST /api/v1/outfit/tryon - 사용자: {}", userDetails.getUser().getUserId());

        OutfitDtos.TryonResponse response = outfitTryonService.generateTryon(
                userDetails.getUser().getUserId(),
                request
        );

        if (response.success()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
