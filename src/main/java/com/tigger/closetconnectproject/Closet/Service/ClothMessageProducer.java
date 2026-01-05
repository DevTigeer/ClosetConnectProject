package com.tigger.closetconnectproject.Closet.Service;

import com.tigger.closetconnectproject.Closet.Dto.ClothProcessingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ 메시지 프로듀서
 * - 옷 이미지 처리 요청을 RabbitMQ 큐에 발행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClothMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.cloth}")
    private String clothExchange;

    @Value("${rabbitmq.routing-key.cloth-processing}")
    private String clothProcessingRoutingKey;

    /**
     * 옷 처리 메시지를 RabbitMQ에 발행
     *
     * @param message 옷 처리 메시지 (clothId, imageBytes, originalFilename)
     */
    public void sendClothProcessingMessage(ClothProcessingMessage message) {
        try {
            log.info("[Producer] Sending cloth processing message to queue: clothId={}", message.getClothId());

            rabbitTemplate.convertAndSend(
                    clothExchange,
                    clothProcessingRoutingKey,
                    message
            );

            log.info("[Producer] Message sent successfully: clothId={}", message.getClothId());

        } catch (Exception e) {
            log.error("[Producer] Failed to send message: clothId={}", message.getClothId(), e);
            throw new RuntimeException("Failed to send cloth processing message", e);
        }
    }
}
