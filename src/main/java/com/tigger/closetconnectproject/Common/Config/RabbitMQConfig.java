package com.tigger.closetconnectproject.Common.Config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 설정
 * - 옷 이미지 처리 파이프라인을 위한 큐, 익스체인지, 바인딩 설정
 * - 메시지 변환, 재시도 정책, 에러 핸들링 설정
 * - spring.rabbitmq.enabled=true일 때만 활성화 (기본값: true)
 */
@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.cloth-processing}")
    private String clothProcessingQueue;

    @Value("${rabbitmq.queue.cloth-result}")
    private String clothResultQueue;

    @Value("${rabbitmq.queue.cloth-progress}")
    private String clothProgressQueue;

    @Value("${rabbitmq.exchange.cloth}")
    private String clothExchange;

    @Value("${rabbitmq.routing-key.cloth-processing}")
    private String clothProcessingRoutingKey;

    @Value("${rabbitmq.routing-key.cloth-result}")
    private String clothResultRoutingKey;

    @Value("${rabbitmq.routing-key.cloth-progress}")
    private String clothProgressRoutingKey;

    /**
     * 옷 처리 요청 큐 선언 (Spring → Python)
     * - durable: true (서버 재시작 시에도 큐 유지)
     * - 메시지 지속성은 메시지 발행 시 설정됨
     */
    @Bean
    public Queue clothProcessingQueue() {
        return QueueBuilder.durable(clothProcessingQueue)
                .build();
    }

    /**
     * 옷 처리 결과 큐 선언 (Python → Spring)
     * - durable: true (서버 재시작 시에도 큐 유지)
     */
    @Bean
    public Queue clothResultQueue() {
        return QueueBuilder.durable(clothResultQueue)
                .build();
    }

    /**
     * 옷 처리 진행도 큐 선언 (Python → Spring)
     * - 실시간 진행 상황 업데이트용
     */
    @Bean
    public Queue clothProgressQueue() {
        return QueueBuilder.durable(clothProgressQueue)
                .build();
    }

    /**
     * 옷 처리 익스체인지 선언 (Direct Exchange)
     * - Direct Exchange: 라우팅 키가 정확히 일치하는 큐로 메시지 전송
     */
    @Bean
    public DirectExchange clothExchange() {
        return new DirectExchange(clothExchange);
    }

    /**
     * 요청 큐와 익스체인지 바인딩
     * - 라우팅 키: cloth.processing
     */
    @Bean
    public Binding clothProcessingBinding(Queue clothProcessingQueue, DirectExchange clothExchange) {
        return BindingBuilder
                .bind(clothProcessingQueue)
                .to(clothExchange)
                .with(clothProcessingRoutingKey);
    }

    /**
     * 응답 큐와 익스체인지 바인딩
     * - 라우팅 키: cloth.result
     */
    @Bean
    public Binding clothResultBinding(Queue clothResultQueue, DirectExchange clothExchange) {
        return BindingBuilder
                .bind(clothResultQueue)
                .to(clothExchange)
                .with(clothResultRoutingKey);
    }

    /**
     * 진행도 큐와 익스체인지 바인딩
     * - 라우팅 키: cloth.progress
     */
    @Bean
    public Binding clothProgressBinding(Queue clothProgressQueue, DirectExchange clothExchange) {
        return BindingBuilder
                .bind(clothProgressQueue)
                .to(clothExchange)
                .with(clothProgressRoutingKey);
    }

    /**
     * 메시지 변환기 (JSON)
     * - Java 객체 <-> JSON 메시지 자동 변환
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 설정
     * - 메시지 발행 시 사용
     * - JSON 컨버터 적용
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                        MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    /**
     * RabbitMQ Listener Container Factory 설정
     * - Consumer 설정 (동시성, prefetch, acknowledge 등)
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        return factory;
    }
}
