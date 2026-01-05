package com.tigger.closetconnectproject.Closet.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigger.closetconnectproject.Closet.Dto.ClothResultMessage;
import com.tigger.closetconnectproject.Closet.Entity.Category;
import com.tigger.closetconnectproject.Closet.Entity.Cloth;
import com.tigger.closetconnectproject.Closet.Entity.ProcessingStatus;
import com.tigger.closetconnectproject.Closet.Repository.ClothRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RabbitMQ 옷 처리 결과 컨슈머 (Python → Spring)
 * - cloth.result.queue에서 메시지를 소비
 * - Python worker가 처리한 결과를 받아서 DB 업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClothResultConsumer {

    private final ClothRepository clothRepository;
    private final ImageStorageService imageStorageService;
    private final ClothProgressNotifier progressNotifier;
    private final ObjectMapper objectMapper;

    /**
     * RabbitMQ 결과 메시지 리스너
     * - 큐: cloth.result.queue
     * - Python worker가 처리 완료 후 전송한 결과 처리
     *
     * @param message 옷 처리 결과 메시지
     */
    @RabbitListener(queues = "${rabbitmq.queue.cloth-result}")
    @Transactional
    public void handleClothResult(ClothResultMessage message) {
        Long clothId = message.getClothId();
        log.info("[ResultConsumer][{}] Received cloth processing result (success: {})",
                clothId, message.getSuccess());

        Cloth cloth = clothRepository.findById(clothId)
                .orElseThrow(() -> new IllegalStateException("Cloth not found: " + clothId));

        Long userId = cloth.getUser().getUserId();

        try {
            if (message.getSuccess()) {
                // === 성공 케이스: Python이 저장한 이미지 파일들을 Spring uploads로 복사 ===
                log.info("[ResultConsumer][{}] Processing successful result", clothId);

                // 1. 배경 제거 이미지 처리
                if (message.getRemovedBgImagePath() != null) {
                    byte[] removedBgBytes = loadImageFile(message.getRemovedBgImagePath());
                    String removedBgUrl = imageStorageService.saveRemovedBgImage(removedBgBytes, clothId);
                    cloth.setRemovedBgImageUrl(removedBgUrl);
                    log.info("[ResultConsumer][{}] Saved removed-bg image: {}", clothId, removedBgUrl);
                }

                // 2. 세그먼트 이미지 처리
                if (message.getSegmentedImagePath() != null) {
                    byte[] segmentedBytes = loadImageFile(message.getSegmentedImagePath());
                    String segmentedUrl = imageStorageService.saveSegmentedImage(segmentedBytes, clothId);
                    cloth.setSegmentedImageUrl(segmentedUrl);
                    log.info("[ResultConsumer][{}] Saved segmented image: {}", clothId, segmentedUrl);
                }

                // 3. 인페인팅 이미지 처리
                if (message.getInpaintedImagePath() != null) {
                    byte[] inpaintedBytes = loadImageFile(message.getInpaintedImagePath());
                    String inpaintedUrl = imageStorageService.saveInpaintedImage(inpaintedBytes, clothId);
                    cloth.setInpaintedImageUrl(inpaintedUrl);
                    log.info("[ResultConsumer][{}] Saved inpainted image: {}", clothId, inpaintedUrl);
                }

                // 4. 메타데이터 저장
                if (message.getSuggestedCategory() != null) {
                    try {
                        Category category = Category.valueOf(message.getSuggestedCategory());
                        cloth.setSuggestedCategory(category);
                    } catch (IllegalArgumentException e) {
                        log.warn("[ResultConsumer][{}] Invalid category: {}, setting to null",
                                clothId, message.getSuggestedCategory());
                        cloth.setSuggestedCategory(null);
                    }
                }
                cloth.setSegmentationLabel(message.getSegmentationLabel());

                // 5. 모든 세그먼트된 아이템들 처리 (크기순 정렬)
                if (message.getAllSegmentedItems() != null && !message.getAllSegmentedItems().isEmpty()) {
                    log.info("[ResultConsumer][{}] Processing {} segmented items",
                            clothId, message.getAllSegmentedItems().size());

                    List<Map<String, Object>> segmentedItemsData = new ArrayList<>();

                    for (ClothResultMessage.SegmentedItem item : message.getAllSegmentedItems()) {
                        try {
                            // 이미지 파일 로드
                            byte[] itemImageBytes = loadImageFile(item.getSegmentedPath());

                            // 이미지 저장
                            String itemImageUrl = imageStorageService.saveSegmentedImage(
                                    itemImageBytes,
                                    clothId,
                                    item.getLabel()
                            );

                            // 메타데이터 구성
                            Map<String, Object> itemData = new HashMap<>();
                            itemData.put("label", item.getLabel());
                            itemData.put("segmentedUrl", itemImageUrl);
                            itemData.put("areaPixels", item.getAreaPixels());

                            segmentedItemsData.add(itemData);

                            log.info("[ResultConsumer][{}] Saved segmented item: {} -> {}",
                                    clothId, item.getLabel(), itemImageUrl);

                        } catch (Exception e) {
                            log.warn("[ResultConsumer][{}] Failed to process segmented item: {}",
                                    clothId, item.getLabel(), e);
                        }
                    }

                    // JSON으로 직렬화하여 저장
                    if (!segmentedItemsData.isEmpty()) {
                        String segmentedItemsJson = objectMapper.writeValueAsString(segmentedItemsData);
                        cloth.setAllSegmentedItemsJson(segmentedItemsJson);
                        log.info("[ResultConsumer][{}] Stored {} segmented items as JSON",
                                clothId, segmentedItemsData.size());
                    }
                }

                // 6. 모든 Gemini 확장된 아이템들 처리 (크기순 정렬)
                if (message.getAllExpandedItems() != null && !message.getAllExpandedItems().isEmpty()) {
                    log.info("[ResultConsumer][{}] Processing {} expanded items",
                            clothId, message.getAllExpandedItems().size());

                    List<Map<String, Object>> expandedItemsData = new ArrayList<>();

                    for (ClothResultMessage.ExpandedItem item : message.getAllExpandedItems()) {
                        try {
                            // 이미지 파일 로드
                            byte[] itemImageBytes = loadImageFile(item.getExpandedPath());

                            // 이미지 저장
                            String itemImageUrl = imageStorageService.saveExpandedImage(
                                    itemImageBytes,
                                    clothId,
                                    item.getLabel()
                            );

                            // 메타데이터 구성
                            Map<String, Object> itemData = new HashMap<>();
                            itemData.put("label", item.getLabel());
                            itemData.put("expandedUrl", itemImageUrl);
                            itemData.put("areaPixels", item.getAreaPixels());

                            expandedItemsData.add(itemData);

                            log.info("[ResultConsumer][{}] Saved expanded item: {} -> {}",
                                    clothId, item.getLabel(), itemImageUrl);

                        } catch (Exception e) {
                            log.warn("[ResultConsumer][{}] Failed to process expanded item: {}",
                                    clothId, item.getLabel(), e);
                        }
                    }

                    // JSON으로 직렬화하여 저장
                    if (!expandedItemsData.isEmpty()) {
                        String expandedItemsJson = objectMapper.writeValueAsString(expandedItemsData);
                        cloth.setAllExpandedItemsJson(expandedItemsJson);
                        log.info("[ResultConsumer][{}] Stored {} expanded items as JSON",
                                clothId, expandedItemsData.size());
                    }
                }

                // 7. 추가 감지된 아이템들 처리 (하위 호환, deprecated)
                if (message.getAdditionalClothingItems() != null && !message.getAdditionalClothingItems().isEmpty()) {
                    log.info("[ResultConsumer][{}] Processing {} additional clothing items (deprecated)",
                            clothId, message.getAdditionalClothingItems().size());

                    List<Map<String, Object>> additionalItemsData = new ArrayList<>();

                    for (ClothResultMessage.AdditionalClothingItem item : message.getAdditionalClothingItems()) {
                        try {
                            // 이미지 파일 로드
                            byte[] itemImageBytes = loadImageFile(item.getPath());

                            // 이미지 저장
                            String itemImageUrl = imageStorageService.saveAdditionalItemImage(
                                    itemImageBytes,
                                    clothId,
                                    item.getLabel()
                            );

                            // 메타데이터 구성
                            Map<String, Object> itemData = new HashMap<>();
                            itemData.put("label", item.getLabel());
                            itemData.put("imageUrl", itemImageUrl);
                            itemData.put("areaPixels", item.getAreaPixels());

                            additionalItemsData.add(itemData);

                            log.info("[ResultConsumer][{}] Saved additional item: {} -> {}",
                                    clothId, item.getLabel(), itemImageUrl);

                        } catch (Exception e) {
                            log.warn("[ResultConsumer][{}] Failed to process additional item: {}",
                                    clothId, item.getLabel(), e);
                        }
                    }

                    // JSON으로 직렬화하여 저장
                    if (!additionalItemsData.isEmpty()) {
                        String additionalItemsJson = objectMapper.writeValueAsString(additionalItemsData);
                        cloth.setAdditionalItemsJson(additionalItemsJson);
                        log.info("[ResultConsumer][{}] Stored {} additional items as JSON (deprecated)",
                                clothId, additionalItemsData.size());
                    }
                }

                // 6. 상태 업데이트: READY_FOR_REVIEW
                cloth.setProcessingStatus(ProcessingStatus.READY_FOR_REVIEW);
                cloth.setErrorMessage(null);
                cloth.setCurrentStep("처리 완료");
                cloth.setProgressPercentage(100);

                clothRepository.save(cloth);

                log.info("[ResultConsumer][{}] ✅ Processing completed successfully - READY_FOR_REVIEW", clothId);

                // 7. WebSocket 완료 알림
                progressNotifier.notifyComplete(userId, clothId);

            } else {
                // === 실패 케이스: 에러 메시지 저장 ===
                log.error("[ResultConsumer][{}] Processing failed: {}", clothId, message.getErrorMessage());

                cloth.setProcessingStatus(ProcessingStatus.FAILED);
                cloth.setErrorMessage(message.getErrorMessage());
                cloth.setCurrentStep("처리 실패");
                cloth.setProgressPercentage(0);

                clothRepository.save(cloth);

                // WebSocket 실패 알림
                progressNotifier.notifyFailure(userId, clothId, message.getErrorMessage());

                log.error("[ResultConsumer][{}] Processing status set to FAILED", clothId);
            }

        } catch (Exception e) {
            log.error("[ResultConsumer][{}] ❌ Failed to process result message", clothId, e);

            // 예외 발생 시 FAILED 상태로 업데이트
            cloth.setProcessingStatus(ProcessingStatus.FAILED);
            cloth.setErrorMessage("결과 처리 중 오류 발생: " + e.getMessage());
            cloth.setCurrentStep("결과 처리 실패");
            cloth.setProgressPercentage(0);
            clothRepository.save(cloth);

            // WebSocket 실패 알림
            progressNotifier.notifyFailure(userId, clothId, e.getMessage());

            // 예외를 다시 던져서 RabbitMQ에 NACK 전송 (재시도 트리거)
            throw new RuntimeException("Failed to process cloth result for clothId: " + clothId, e);
        }
    }

    /**
     * Python이 저장한 이미지 파일을 읽어서 바이트 배열로 반환
     *
     * @param imagePath Python 서버가 저장한 이미지 파일 경로
     * @return 이미지 바이트 배열
     */
    private byte[] loadImageFile(String imagePath) {
        try {
            Path path = Paths.get(imagePath);

            if (!Files.exists(path)) {
                throw new IllegalStateException("Image file not found: " + imagePath);
            }

            byte[] imageBytes = Files.readAllBytes(path);
            log.debug("Loaded image file: {} ({} bytes)", imagePath, imageBytes.length);

            return imageBytes;

        } catch (IOException e) {
            log.error("Failed to load image file: {}", imagePath, e);
            throw new RuntimeException("이미지 파일 로드 실패: " + imagePath, e);
        }
    }
}
