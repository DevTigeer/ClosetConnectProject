package com.tigger.closetconnectproject.Common.Config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * WebClient 및 RestTemplate 설정
 * - rembg 서버 연동 등 외부 API 호출에 사용
 * - 토스페이먼츠 WebClient는 Market/Config/TossPaymentConfig에서 설정
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
    }
}
