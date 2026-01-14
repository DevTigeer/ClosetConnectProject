package com.tigger.closetconnectproject.Closet.Service;

import com.tigger.closetconnectproject.Closet.Dto.*;
import com.tigger.closetconnectproject.Closet.Entity.Category;
import com.tigger.closetconnectproject.Closet.Entity.Cloth;
import com.tigger.closetconnectproject.Closet.Entity.ProcessingStatus;
import com.tigger.closetconnectproject.Closet.Event.ClothUploadedEvent;
import com.tigger.closetconnectproject.Closet.Repository.ClothRepository;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ImageStorageService imageStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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
     * 이미지 업로드와 함께 옷 생성 (비동기 처리)
     * - 원본 이미지 즉시 저장
     * - 비동기 파이프라인 시작 (rembg → segmentation → inpainting)
     * - PROCESSING 상태로 즉시 응답 반환
     *
     * @param userId 사용자 ID
     * @param imageFile 업로드된 이미지 파일
     * @param req 옷 정보 (name, category는 optional - AI가 제안)
     * @return 생성된 옷 정보 (PROCESSING 상태)
     */
    @Transactional
    public ClothResponse createWithImage(Long userId, MultipartFile imageFile, ClothUploadRequest req) {
        log.info("Creating cloth with async processing for userId: {}, name: {}", userId, req.getName());

        // 1. 사용자 조회
        Users owner = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. Cloth 엔티티 생성 (category=null, processingStatus=PROCESSING)
        Cloth cloth = Cloth.builder()
                .user(owner)
                .name(req.getName())
                .category(req.getCategory())  // null 가능 (AI가 제안할 예정)
                .processingStatus(ProcessingStatus.PROCESSING)
                .build();
        Cloth savedCloth = clothRepository.save(cloth);
        Long clothId = savedCloth.getId();

        try {
            // 3. 원본 이미지 즉시 저장
            String originalImageUrl = imageStorageService.saveOriginalImage(imageFile, clothId);
            savedCloth.setOriginalImageUrl(originalImageUrl);
            clothRepository.save(savedCloth);
            clothRepository.flush();  // 트랜잭션 커밋 전에 DB에 즉시 반영
            log.info("[{}] Original image saved: {}", clothId, originalImageUrl);

            // 4. 이벤트 발행 (트랜잭션 커밋 후 비동기 처리 시작)
            byte[] imageBytes = imageFile.getBytes();
            String imageType = req.getImageType() != null ? req.getImageType().name() : "FULL_BODY";
            eventPublisher.publishEvent(new ClothUploadedEvent(this, clothId, userId, imageBytes, imageFile.getOriginalFilename(), imageType));
            log.info("[{}] ClothUploadedEvent published for userId: {}, imageType: {}", clothId, userId, imageType);

            // 5. 즉시 응답 반환 (PROCESSING 상태)
            return toDto(savedCloth);

        } catch (Exception e) {
            log.error("[{}] Failed to initiate cloth processing", clothId, e);

            // 롤백: 저장된 파일 및 엔티티 삭제
            imageStorageService.deleteImage(savedCloth.getOriginalImageUrl());
            clothRepository.delete(savedCloth);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "옷 등록 실패: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 처리 상태 조회
     *
     * @param userId 사용자 ID
     * @param clothId 옷 ID
     * @return 처리 상태 정보
     */
    @Transactional(readOnly = true)
    public ClothStatusResponse getStatus(Long userId, Long clothId) {
        Cloth cloth = clothRepository.findById(clothId)
                .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다."));

        if (!cloth.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("본인 소유가 아닙니다.");
        }

        return new ClothStatusResponse(
                cloth.getId(),
                cloth.getProcessingStatus(),
                cloth.getCurrentStep(),
                cloth.getProgressPercentage(),
                cloth.getSuggestedCategory(),
                cloth.getSegmentationLabel(),
                cloth.getSegmentedImageUrl(),
                cloth.getInpaintedImageUrl(),
                cloth.getErrorMessage()
        );
    }

    /**
     * 카테고리 확인 (AI 제안 후 사용자 확인/수정) - OK 선택
     *
     * @param userId 사용자 ID
     * @param clothId 옷 ID
     * @param req 확인된 카테고리
     * @return 최종 확정된 옷 정보
     */
    @Transactional
    public ClothResponse confirmCategory(Long userId, Long clothId, ConfirmCategoryRequest req) {
        Cloth cloth = clothRepository.findById(clothId)
                .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다."));

        if (!cloth.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("본인 소유가 아닙니다.");
        }

        if (cloth.getProcessingStatus() != ProcessingStatus.READY_FOR_REVIEW) {
            throw new IllegalStateException("처리가 완료되지 않았습니다. 현재 상태: " + cloth.getProcessingStatus());
        }

        // 사용자가 확인한 카테고리 설정 (AI 제안 또는 사용자 수정)
        cloth.setCategory(req.category());

        // 사용자가 선택한 이미지를 최종 이미지로 설정
        String selectedImageUrl = switch (req.selectedImageType()) {
            case ORIGINAL -> cloth.getOriginalImageUrl();
            case REMOVED_BG -> cloth.getRemovedBgImageUrl();
            case SEGMENTED -> cloth.getSegmentedImageUrl();
            case INPAINTED -> cloth.getInpaintedImageUrl();
        };

        if (selectedImageUrl == null) {
            throw new IllegalStateException("선택한 이미지가 존재하지 않습니다: " + req.selectedImageType());
        }

        cloth.setImageUrl(selectedImageUrl);

        // 상태를 COMPLETED로 변경
        cloth.setProcessingStatus(ProcessingStatus.COMPLETED);

        log.info("[{}] Category confirmed: {}, selected image: {}, status: COMPLETED",
                clothId, req.category(), req.selectedImageType());

        return toDto(cloth);
    }

    /**
     * 처리 결과 거부 (NO 선택) - 옷 삭제
     *
     * @param userId 사용자 ID
     * @param clothId 옷 ID
     */
    @Transactional
    public void rejectCloth(Long userId, Long clothId) {
        Cloth cloth = clothRepository.findById(clothId)
                .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다."));

        if (!cloth.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("본인 소유가 아닙니다.");
        }

        // 이미지 파일 삭제
        imageStorageService.deleteImage(cloth.getOriginalImageUrl());
        imageStorageService.deleteImage(cloth.getRemovedBgImageUrl());
        imageStorageService.deleteImage(cloth.getSegmentedImageUrl());
        imageStorageService.deleteImage(cloth.getInpaintedImageUrl());

        // DB에서 삭제
        clothRepository.delete(cloth);

        log.info("[{}] Cloth rejected and deleted by user", clothId);
    }

    @Transactional(readOnly = true)
    public Page<ClothResponse> list(Long userId, Category category, Pageable pageable) {
        // confirmed=true인 옷만 조회 (사용자가 최종 이미지를 선택한 것만)
        Page<Cloth> page = (category == null)
                ? clothRepository.findByUser_UserIdAndConfirmedTrue(userId, pageable)
                : clothRepository.findByUser_UserIdAndCategoryAndConfirmedTrue(userId, category, pageable);
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

    /**
     * 사용자가 최종 이미지를 선택하여 옷을 확정합니다.
     * AI 처리가 완료된 후, 사용자가 원하는 이미지를 선택하면 호출됩니다.
     */
    @Transactional
    public ClothResponse confirmCloth(Long userId, Long clothId, ConfirmClothRequest request) {
        Cloth cloth = clothRepository.findById(clothId)
                .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다."));

        // 권한 확인
        if (!cloth.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("본인 소유가 아닙니다.");
        }

        // 선택 검증
        if (!request.hasValidSelection()) {
            throw new IllegalArgumentException("이미지를 선택해주세요.");
        }

        // 선택한 이미지 URL 결정
        String selectedImageUrl;
        if (request.selectedImageUrl() != null && !request.selectedImageUrl().isBlank()) {
            // 직접 제공된 URL 사용 (추가 아이템)
            selectedImageUrl = request.selectedImageUrl();
        } else {
            // 표준 이미지 타입에서 선택
            selectedImageUrl = switch (request.selectedImageType()) {
                case ORIGINAL -> cloth.getOriginalImageUrl();
                case REMOVED_BG -> cloth.getRemovedBgImageUrl();
                case SEGMENTED -> cloth.getSegmentedImageUrl();
                case INPAINTED -> cloth.getInpaintedImageUrl();
            };
        }

        if (selectedImageUrl == null) {
            throw new IllegalArgumentException("선택한 이미지가 존재하지 않습니다.");
        }

        cloth.setImageUrl(selectedImageUrl);

        // 카테고리 업데이트 (선택적)
        if (request.category() != null) {
            cloth.setCategory(request.category());
        } else if (cloth.getSuggestedCategory() != null) {
            // 카테고리를 지정하지 않았으면 AI 제안 카테고리 사용
            cloth.setCategory(cloth.getSuggestedCategory());
        }

        // confirmed = true로 설정 (이제 옷장에 표시됨)
        cloth.setConfirmed(true);

        Cloth saved = clothRepository.save(cloth);

        log.info("[{}] Cloth confirmed by user: imageType={}, category={}",
                clothId, request.selectedImageType(), cloth.getCategory());

        return toDto(saved);
    }

    /**
     * JSON 문자열을 파싱하여 리스트로 변환하는 범용 메서드
     *
     * @param json JSON 문자열
     * @param mapper Map을 DTO로 변환하는 함수
     * @param clothId 로깅용 옷 ID
     * @param jsonType 로깅용 JSON 타입 설명
     * @return 파싱된 DTO 리스트, 실패 시 빈 리스트
     */
    private <T> java.util.List<T> parseJsonToList(
            String json,
            java.util.function.Function<java.util.Map<String, Object>, T> mapper,
            Long clothId,
            String jsonType
    ) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            java.util.List<java.util.Map<String, Object>> itemsData = objectMapper.readValue(
                    json,
                    new com.fasterxml.jackson.core.type.TypeReference<>() {}
            );
            return itemsData.stream()
                    .map(mapper)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse {} for cloth {}: {}", jsonType, clothId, e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 추가 아이템 JSON 파싱
     */
    private java.util.List<ClothResponse.AdditionalItemResponse> parseAdditionalItems(Cloth c) {
        return parseJsonToList(
                c.getAdditionalItemsJson(),
                itemData -> new ClothResponse.AdditionalItemResponse(
                        (String) itemData.get("label"),
                        (String) itemData.get("imageUrl"),
                        (Integer) itemData.get("areaPixels")
                ),
                c.getId(),
                "additionalItemsJson"
        );
    }

    /**
     * 세그먼트된 아이템 JSON 파싱
     */
    private java.util.List<ClothResponse.SegmentedItemResponse> parseSegmentedItems(Cloth c) {
        return parseJsonToList(
                c.getAllSegmentedItemsJson(),
                itemData -> new ClothResponse.SegmentedItemResponse(
                        (String) itemData.get("label"),
                        (String) itemData.get("segmentedUrl"),
                        (Integer) itemData.get("areaPixels")
                ),
                c.getId(),
                "allSegmentedItemsJson"
        );
    }

    /**
     * 확장된 아이템 JSON 파싱
     */
    private java.util.List<ClothResponse.ExpandedItemResponse> parseExpandedItems(Cloth c) {
        return parseJsonToList(
                c.getAllExpandedItemsJson(),
                itemData -> new ClothResponse.ExpandedItemResponse(
                        (String) itemData.get("label"),
                        (String) itemData.get("expandedUrl"),
                        (Integer) itemData.get("areaPixels")
                ),
                c.getId(),
                "allExpandedItemsJson"
        );
    }

    private ClothResponse toDto(Cloth c) {
        // 이미지 우선순위: imageUrl > inpaintedImageUrl > segmentedImageUrl > originalImageUrl
        String displayImageUrl = getDisplayImageUrl(c);

        // JSON 파싱 (helper 메서드 사용)
        java.util.List<ClothResponse.AdditionalItemResponse> additionalItems = parseAdditionalItems(c);
        java.util.List<ClothResponse.SegmentedItemResponse> allSegmentedItems = parseSegmentedItems(c);
        java.util.List<ClothResponse.ExpandedItemResponse> allExpandedItems = parseExpandedItems(c);

        return new ClothResponse(
                c.getId(),
                c.getUser().getUserId(),  // WebSocket 구독 경로를 위한 userId
                c.getName(),
                c.getCategory(),
                displayImageUrl,  // 화면에 표시할 이미지
                c.getOriginalImageUrl(),
                c.getRemovedBgImageUrl(),
                c.getSegmentedImageUrl(),
                c.getInpaintedImageUrl(),
                c.getProcessingStatus(),
                c.getSuggestedCategory(),
                c.getSegmentationLabel(),
                c.getErrorMessage(),
                additionalItems,  // 추가 아이템 (deprecated)
                allSegmentedItems,  // 모든 세그먼트된 아이템 (크기순)
                allExpandedItems  // 모든 Gemini 확장된 아이템 (크기순)
        );
    }

    /**
     * 화면에 표시할 이미지 URL 결정
     * - COMPLETED: imageUrl (사용자가 확인한 최종 이미지)
     * - READY_FOR_REVIEW: inpaintedImageUrl (AI 처리 완료된 이미지)
     * - PROCESSING: originalImageUrl (처리 중이므로 원본만 표시)
     * - FAILED: originalImageUrl
     *
     * @return 이미지 URL (null일 수 있음 - 프론트엔드에서 placeholder 처리)
     */
    private String getDisplayImageUrl(Cloth c) {
        // 최종 확정된 이미지가 있으면 사용
        if (c.getImageUrl() != null) {
            return c.getImageUrl();
        }

        // 처리 상태에 따라 적절한 이미지 반환
        ProcessingStatus status = c.getProcessingStatus();

        if (status == ProcessingStatus.READY_FOR_REVIEW || status == ProcessingStatus.COMPLETED) {
            // AI 처리 완료: inpainting (최종) > segmented > removedBg > original 순서
            if (c.getInpaintedImageUrl() != null) {
                return c.getInpaintedImageUrl();
            }
            if (c.getSegmentedImageUrl() != null) {
                return c.getSegmentedImageUrl();
            }
            if (c.getRemovedBgImageUrl() != null) {
                return c.getRemovedBgImageUrl();
            }
        }

        // 처리 중이거나 실패한 경우: 원본 이미지 표시
        String originalUrl = c.getOriginalImageUrl();

        // null 체크 및 로깅 (디버깅용)
        if (originalUrl == null) {
            log.warn("[clothId={}] All image URLs are null! Status: {}, Name: {}",
                c.getId(), c.getProcessingStatus(), c.getName());
        }

        return originalUrl;  // null일 수 있음 (프론트엔드에서 placeholder 처리)
    }
}

