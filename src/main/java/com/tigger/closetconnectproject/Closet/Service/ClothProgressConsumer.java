package com.tigger.closetconnectproject.Closet.Service;

import com.tigger.closetconnectproject.Closet.Dto.ClothProgressMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ 옷 처리 진행도 컨슈머 (Python → Spring)
 * - cloth.progress.queue에서 실시간 진행 상황 메시지를 소비
 * - WebSocket을 통해 프론트엔드로 진행도 전송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClothProgressConsumer {

    private final ClothProgressNotifier progressNotifier;

    /**
     * RabbitMQ 진행도 메시지 리스너
     * - 큐: cloth.progress.queue
     * - Python worker가 처리 중 실시간으로 전송하는 진행 상황 처리
     *
     * @param message 옷 처리 진행도 메시지
     */
    @RabbitListener(queues = "${rabbitmq.queue.cloth-progress}")
    public void handleProgressUpdate(ClothProgressMessage message) {
        log.info("[ProgressConsumer] Received progress update: clothId={}, userId={}, step={}, progress={}%",
                message.getClothId(), message.getUserId(), message.getCurrentStep(), message.getProgressPercentage());

        // WebSocket을 통해 프론트엔드로 진행도 전송
        progressNotifier.notifyProgress(
                message.getUserId(),
                message.getClothId(),
                message.getStatus(),
                message.getCurrentStep(),
                message.getProgressPercentage()
        );

        log.info("[ProgressConsumer] ✅ Progress forwarded to WebSocket: {}%", message.getProgressPercentage());
    }
}
