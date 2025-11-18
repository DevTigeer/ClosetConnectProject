package com.tigger.closetconnectproject.Market.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 토스페이먼츠 WebClient 설정
 */
@Slf4j
@Configuration
public class TossPaymentConfig {

    private final TossPaymentProperties properties;

    public TossPaymentConfig(TossPaymentProperties properties) {
        this.properties = properties;
        // 애플리케이션 시작 시 설정 유효성 검증
        properties.validate();

        // 설정 정보 출력 (테스트용)
        log.info("========================================");
        log.info("토스페이먼츠 설정 정보");
        log.info("========================================");
        log.info("클라이언트 키: {}", properties.getClientKey());
        log.info("시크릿 키: {}", properties.getSecretKey());
        log.info("API URL: {}", properties.getApiUrl());
        log.info("인코딩된 시크릿 키: {}", properties.getEncodedSecretKey());
        log.info("테스트 모드: {}", properties.isTestMode());
        log.info("========================================");
    }

    /**
     * 토스페이먼츠 API 호출용 WebClient
     *
     * Base URL과 기본 헤더를 설정
     */
    @Bean
    public WebClient tossPaymentWebClient() {
        return WebClient.builder()
                .baseUrl(properties.getApiUrl())
                .defaultHeader("Authorization", "Basic " + properties.getEncodedSecretKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
