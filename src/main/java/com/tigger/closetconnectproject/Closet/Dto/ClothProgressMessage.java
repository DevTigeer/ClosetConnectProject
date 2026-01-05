package com.tigger.closetconnectproject.Closet.Dto;

import com.tigger.closetconnectproject.Closet.Entity.ProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 옷 처리 진행률 메시지 (Spring → Frontend)
 * - 실시간으로 프론트엔드에 진행 상황 전달
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClothProgressMessage {

    private Long clothId;
    private Long userId;
    private ProcessingStatus status;
    private String currentStep;
    private Integer progressPercentage;
    private String errorMessage;
    private Long timestamp;

    public ClothProgressMessage(Long clothId, Long userId, ProcessingStatus status,
                                String currentStep, Integer progressPercentage) {
        this.clothId = clothId;
        this.userId = userId;
        this.status = status;
        this.currentStep = currentStep;
        this.progressPercentage = progressPercentage;
        this.timestamp = System.currentTimeMillis();
    }
}
