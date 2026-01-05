package com.tigger.closetconnectproject.Closet.Service;

import com.tigger.closetconnectproject.Closet.Dto.ClothProgressMessage;
import com.tigger.closetconnectproject.Closet.Entity.ProcessingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 옷 처리 진행 상황 WebSocket 알림 서비스
 * - 클라이언트에게 실시간으로 진행 상황을 전송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClothProgressNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 특정 사용자에게 진행 상황 알림 전송
     *
     * @param userId 사용자 ID
     * @param clothId 옷 ID
     * @param status 처리 상태
     * @param currentStep 현재 단계
     * @param progressPercentage 진행률 (0-100)
     */
    public void notifyProgress(Long userId, Long clothId, ProcessingStatus status,
                              String currentStep, Integer progressPercentage) {
        ClothProgressMessage message = new ClothProgressMessage(
                clothId,
                userId,
                status,
                currentStep,
                progressPercentage
        );

        // 특정 사용자에게만 메시지 전송 (/queue/cloth/progress)
        String destination = "/queue/cloth/progress/" + userId;
        messagingTemplate.convertAndSend(destination, message);

        log.info("[WebSocket] 📡 Sent progress to user {}: clothId={}, step={}, progress={}%",
                userId, clothId, currentStep, progressPercentage);
    }

    /**
     * 실패 알림 전송
     *
     * @param userId 사용자 ID
     * @param clothId 옷 ID
     * @param errorMessage 에러 메시지
     */
    public void notifyFailure(Long userId, Long clothId, String errorMessage) {
        ClothProgressMessage message = new ClothProgressMessage();
        message.setClothId(clothId);
        message.setUserId(userId);
        message.setStatus(ProcessingStatus.FAILED);
        message.setCurrentStep("처리 실패");
        message.setProgressPercentage(0);
        message.setErrorMessage(errorMessage);
        message.setTimestamp(System.currentTimeMillis());

        String destination = "/queue/cloth/progress/" + userId;
        messagingTemplate.convertAndSend(destination, message);

        log.info("[WebSocket] ⚠️  Sent failure notification to user {}: clothId={}, error={}",
                userId, clothId, errorMessage);
    }

    /**
     * 완료 알림 전송
     *
     * @param userId 사용자 ID
     * @param clothId 옷 ID
     */
    public void notifyComplete(Long userId, Long clothId) {
        ClothProgressMessage message = new ClothProgressMessage(
                clothId,
                userId,
                ProcessingStatus.READY_FOR_REVIEW,
                "처리 완료",
                100
        );

        String destination = "/queue/cloth/progress/" + userId;
        messagingTemplate.convertAndSend(destination, message);

        log.info("[WebSocket] ✅ Sent completion notification to user {}: clothId={}, destination={}",
                userId, clothId, destination);
    }
}
