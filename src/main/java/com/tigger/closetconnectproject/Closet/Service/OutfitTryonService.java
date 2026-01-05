package com.tigger.closetconnectproject.Closet.Service;

import com.tigger.closetconnectproject.Closet.Client.TryonClient;
import com.tigger.closetconnectproject.Closet.Dto.OutfitDtos;
import com.tigger.closetconnectproject.Closet.Entity.Cloth;
import com.tigger.closetconnectproject.Closet.Repository.ClothRepository;
import com.tigger.closetconnectproject.Closet.Service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Outfit Try-On 서비스
 * 의류 아이템들을 조합하여 가상 착용 이미지 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutfitTryonService {

    private final TryonClient tryonClient;
    private final ClothRepository clothRepository;
    private final ImageStorageService imageStorageService;

    /**
     * Outfit Try-On 이미지 생성
     *
     * @param userId 사용자 ID
     * @param request Try-On 요청 DTO
     * @return Try-On 응답 DTO
     */
    @Transactional(readOnly = true)
    public OutfitDtos.TryonResponse generateTryon(Long userId, OutfitDtos.CreateTryonRequest request) {
        log.info("사용자 {}의 Try-On 생성 시작", userId);

        // Try-On 서비스 사용 가능 여부 확인
        if (!tryonClient.isAvailable()) {
            log.error("Try-On 서비스를 사용할 수 없습니다.");
            return OutfitDtos.TryonResponse.failure("Try-On 서비스를 사용할 수 없습니다.");
        }

        try {
            // 의류 아이템 조회 및 권한 확인
            Cloth upperClothes = null;
            Cloth lowerClothes = null;
            Cloth shoes = null;
            List<Cloth> accessories = new ArrayList<>();

            // 상의
            if (request.upperClothesId() != null) {
                upperClothes = clothRepository.findById(request.upperClothesId())
                        .orElseThrow(() -> new IllegalArgumentException("상의를 찾을 수 없습니다: " + request.upperClothesId()));
                validateClothOwnership(upperClothes, userId);
                log.info("상의 조회: {}", upperClothes.getName());
            }

            // 하의
            if (request.lowerClothesId() != null) {
                lowerClothes = clothRepository.findById(request.lowerClothesId())
                        .orElseThrow(() -> new IllegalArgumentException("하의를 찾을 수 없습니다: " + request.lowerClothesId()));
                validateClothOwnership(lowerClothes, userId);
                log.info("하의 조회: {}", lowerClothes.getName());
            }

            // 신발
            if (request.shoesId() != null) {
                shoes = clothRepository.findById(request.shoesId())
                        .orElseThrow(() -> new IllegalArgumentException("신발을 찾을 수 없습니다: " + request.shoesId()));
                validateClothOwnership(shoes, userId);
                log.info("신발 조회: {}", shoes.getName());
            }

            // 악세서리
            if (request.accessoriesIds() != null && !request.accessoriesIds().isEmpty()) {
                for (Long accessoryId : request.accessoriesIds()) {
                    Cloth accessory = clothRepository.findById(accessoryId)
                            .orElseThrow(() -> new IllegalArgumentException("악세서리를 찾을 수 없습니다: " + accessoryId));
                    validateClothOwnership(accessory, userId);
                    accessories.add(accessory);
                }
                log.info("악세서리 {}개 조회", accessories.size());
            }

            // Try-On 생성
            String imageDataUrl = tryonClient.generateTryon(
                    upperClothes,
                    lowerClothes,
                    shoes,
                    accessories.isEmpty() ? null : accessories,
                    request.prompt()
            );

            // Base64 이미지를 파일로 저장
            String savedImageUrl = saveBase64Image(imageDataUrl, userId);
            log.info("Try-On 이미지 저장 완료: {}", savedImageUrl);

            return OutfitDtos.TryonResponse.success(savedImageUrl, tryonClient.getEngineName());

        } catch (IllegalArgumentException e) {
            log.error("Try-On 생성 실패: {}", e.getMessage());
            return OutfitDtos.TryonResponse.failure(e.getMessage());
        } catch (Exception e) {
            log.error("Try-On 생성 중 오류 발생", e);
            return OutfitDtos.TryonResponse.failure("Try-On 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 의류 아이템의 소유권 확인
     *
     * @param cloth 의류 아이템
     * @param userId 사용자 ID
     */
    private void validateClothOwnership(Cloth cloth, Long userId) {
        if (!cloth.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 의류에 접근할 권한이 없습니다: " + cloth.getId());
        }
    }

    /**
     * Base64 이미지 데이터를 파일로 저장
     *
     * @param dataUrl Base64 데이터 URL (data:image/png;base64,...)
     * @param userId 사용자 ID
     * @return 저장된 이미지 URL
     */
    private String saveBase64Image(String dataUrl, Long userId) {
        try {
            // data:image/png;base64, 접두사 제거
            String base64Data = dataUrl;
            if (dataUrl.contains(",")) {
                base64Data = dataUrl.split(",", 2)[1];
            }

            // Base64 디코딩
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // 파일명 생성
            String fileName = String.format("tryon_%d_%d.png", userId, System.currentTimeMillis());

            // 이미지 저장
            return imageStorageService.saveImageBytes(imageBytes, fileName);

        } catch (Exception e) {
            log.error("이미지 저장 실패", e);
            throw new RuntimeException("이미지 저장 실패: " + e.getMessage(), e);
        }
    }
}
