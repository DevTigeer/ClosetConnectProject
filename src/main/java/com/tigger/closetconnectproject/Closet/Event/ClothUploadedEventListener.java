package com.tigger.closetconnectproject.Closet.Event;

import com.tigger.closetconnectproject.Closet.Dto.ClothProcessingMessage;
import com.tigger.closetconnectproject.Closet.Service.ClothMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Cloth 업로드 이벤트 리스너
 * - 트랜잭션 커밋 후에 RabbitMQ 메시지 발행
 * - RabbitMQ Consumer가 메시지를 수신하여 비동기 처리 시작
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClothUploadedEventListener {

    private final ClothMessageProducer clothMessageProducer;

    /**
     * 트랜잭션 커밋 후 RabbitMQ 메시지 발행
     * - AFTER_COMMIT: 트랜잭션이 성공적으로 커밋된 후에만 실행
     * - 이때는 DB에 Cloth가 이미 저장되어 있으므로 조회 가능
     * - RabbitMQ에 메시지를 발행하여 Consumer가 처리하도록 함
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleClothUploaded(ClothUploadedEvent event) {
        log.info("[{}] Publishing RabbitMQ message after transaction commit", event.getClothId());

        ClothProcessingMessage message = new ClothProcessingMessage(
                event.getClothId(),
                event.getUserId(),
                event.getImageBytes(),
                event.getOriginalFilename(),
                event.getImageType(),
                0,  // 초기 재시도 횟수
                System.currentTimeMillis()  // 타임스탬프 (밀리초)
        );

        clothMessageProducer.sendClothProcessingMessage(message);
    }
}
