package com.tigger.closetconnectproject.Closet.Service;

import com.tigger.closetconnectproject.Closet.Client.RembgClient;
import com.tigger.closetconnectproject.Closet.Dto.ClothCreateRequest;
import com.tigger.closetconnectproject.Closet.Dto.ClothResponse;
import com.tigger.closetconnectproject.Closet.Dto.ClothUploadRequest;
import com.tigger.closetconnectproject.Closet.Entity.Category;
import com.tigger.closetconnectproject.Closet.Entity.Cloth;
import com.tigger.closetconnectproject.Closet.Repository.ClothRepository;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothService {

    private final ClothRepository clothRepository;
    private final UsersRepository usersRepository;
    private final RembgClient rembgClient;
    private final ImageStorageService imageStorageService;

    @Transactional
    public ClothResponse create(Long userId, ClothCreateRequest req) {
        Users owner = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        Cloth saved = clothRepository.save(
                Cloth.builder()
                        .user(owner)
                        .name(req.name())
                        .category(req.category())
                        .imageUrl(req.imageUrl())
                        .build()
        );
        return toDto(saved);
    }

    /**
     * 이미지 업로드와 함께 옷 생성
     * - 원본 이미지 저장
     * - rembg 서버로 배경 제거 요청
     * - 배경 제거 이미지 저장
     * - DB에 저장
     *
     * @param userId 사용자 ID
     * @param imageFile 업로드된 이미지 파일
     * @param req 옷 정보 (name, category)
     * @return 생성된 옷 정보
     */
    @Transactional
    public ClothResponse createWithImage(Long userId, MultipartFile imageFile, ClothUploadRequest req) {
        log.info("Creating cloth with image upload for userId: {}, name: {}", userId, req.getName());

        // 1. 사용자 조회
        Users owner = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 임시 Cloth 엔티티 생성 (ID 생성을 위해)
        Cloth cloth = Cloth.builder()
                .user(owner)
                .name(req.getName())
                .category(req.getCategory())
                .build();
        Cloth savedCloth = clothRepository.save(cloth);
        Long clothId = savedCloth.getId();

        String originalImageUrl = null;
        String removedBgImageUrl = null;

        try {
            // 3. 원본 이미지 저장
            originalImageUrl = imageStorageService.saveOriginalImage(imageFile, clothId);
            log.info("Saved original image: {}", originalImageUrl);

            // 4. rembg 서버로 배경 제거 시도 (실패해도 계속 진행)
            try {
                byte[] removedBgImageBytes = rembgClient.removeBackground(imageFile);
                log.info("Received background-removed image: {} bytes", removedBgImageBytes.length);

                // 5. 배경 제거 이미지 저장
                removedBgImageUrl = imageStorageService.saveRemovedBgImage(removedBgImageBytes, clothId);
                log.info("Saved removed-bg image: {}", removedBgImageUrl);

                // 6-1. 배경 제거 성공: 누끼 이미지를 기본 이미지로 설정
                savedCloth.setImageUrl(removedBgImageUrl);
                savedCloth.setRemovedBgImageUrl(removedBgImageUrl);

            } catch (Exception rembgException) {
                // rembg 서버 실패 시: 원본 이미지만 사용
                log.warn("Background removal failed, using original image only: {}", rembgException.getMessage());

                // 6-2. 배경 제거 실패: 원본 이미지를 기본 이미지로 설정
                savedCloth.setImageUrl(originalImageUrl);
                savedCloth.setRemovedBgImageUrl(null);
            }

            // 원본 이미지 URL은 항상 설정
            savedCloth.setOriginalImageUrl(originalImageUrl);

            return toDto(savedCloth);

        } catch (Exception e) {
            log.error("Failed to create cloth with image", e);

            // 롤백: 업로드된 파일 삭제
            if (originalImageUrl != null) {
                imageStorageService.deleteImage(originalImageUrl);
            }
            if (removedBgImageUrl != null) {
                imageStorageService.deleteImage(removedBgImageUrl);
            }

            // Cloth 엔티티도 삭제
            clothRepository.delete(savedCloth);

            // 예외 재전송
            if (e instanceof ResponseStatusException) {
                throw (ResponseStatusException) e;
            } else {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "옷 등록 실패: " + e.getMessage(),
                        e
                );
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<ClothResponse> list(Long userId, Category category, Pageable pageable) {
        Page<Cloth> page = (category == null)
                ? clothRepository.findByUser_UserId(userId, pageable)
                : clothRepository.findByUser_UserIdAndCategory(userId, category, pageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ClothResponse getOne(Long userId, Long clothId) {
        Cloth c = clothRepository.findById(clothId)
                .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다."));
        if (!c.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("본인 소유가 아닙니다.");
        }
        return toDto(c);
    }

    @Transactional
    public void delete(Long userId, Long clothId) {
        Cloth c = clothRepository.findById(clothId)
                .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다."));
        if (!c.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("본인 소유가 아닙니다.");
        }
        clothRepository.delete(c);
    }

    private ClothResponse toDto(Cloth c) {
        return new ClothResponse(
                c.getId(),
                c.getName(),
                c.getCategory(),
                c.getImageUrl(),
                c.getOriginalImageUrl(),
                c.getRemovedBgImageUrl()
        );
    }
}

