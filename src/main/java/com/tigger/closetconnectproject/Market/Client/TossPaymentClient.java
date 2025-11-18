package com.tigger.closetconnectproject.Market.Client;

import com.tigger.closetconnectproject.Market.Config.TossPaymentProperties;
import com.tigger.closetconnectproject.Market.Dto.PaymentDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 토스페이먼츠 API 클라이언트
 * WebClient를 사용한 HTTP 통신
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TossPaymentClient {

    private final WebClient tossPaymentWebClient;
    private final TossPaymentProperties properties;

    /**
     * 결제 승인 API 호출
     *
     * @param request 결제 승인 요청 (paymentKey, orderId, amount)
     * @return 토스 결제 응답
     * @throws RuntimeException 결제 승인 실패 시
     */
    public PaymentDtos.TossPaymentResponse confirmPayment(PaymentDtos.ConfirmRequest request) {
        log.info("[결제승인 요청] 주문번호={}, 금액={}, paymentKey={}",
                request.orderId(), request.amount(), request.paymentKey());
        log.info("[인증정보 확인] 클라이언트키={}, 시크릿키={}, API_URL={}",
                properties.getClientKey(),
                properties.getSecretKey(),
                properties.getApiUrl());
        log.info("[요청 헤더] Authorization: Basic {}",
                properties.getEncodedSecretKey());

        try {
            PaymentDtos.TossPaymentResponse response = tossPaymentWebClient
                    .post()
                    .uri("/payments/confirm")
                    .bodyValue(Map.of(
                            "paymentKey", request.paymentKey(),
                            "orderId", request.orderId(),
                            "amount", request.amount()
                    ))
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("[결제승인 실패] 주문번호={}, 에러={}", request.orderId(), errorBody);
                                        return Mono.error(new RuntimeException("결제 승인 실패: " + errorBody));
                                    })
                    )
                    .bodyToMono(PaymentDtos.TossPaymentResponse.class)
                    .block();

            log.info("[결제승인 성공] MID={}, 주문번호={}, paymentKey={}, 결제수단={}, 금액={}",
                    response.mId(), response.orderId(), response.paymentKey(),
                    response.method(), response.totalAmount());
            return response;

        } catch (Exception e) {
            log.error("[결제승인 오류] 주문번호={}, 오류={}", request.orderId(), e.getMessage(), e);
            throw new RuntimeException("결제 승인 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 결제 취소 API 호출
     *
     * @param paymentKey 결제 키
     * @param cancelReason 취소 사유
     * @return 취소 응답
     */
    public PaymentDtos.TossPaymentResponse cancelPayment(String paymentKey, String cancelReason) {
        log.info("[결제취소 요청] paymentKey={}, 취소사유={}", paymentKey, cancelReason);

        try {
            PaymentDtos.TossPaymentResponse response = tossPaymentWebClient
                    .post()
                    .uri("/payments/{paymentKey}/cancel", paymentKey)
                    .bodyValue(Map.of("cancelReason", cancelReason))
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("[결제취소 실패] paymentKey={}, 에러={}", paymentKey, errorBody);
                                        return Mono.error(new RuntimeException("결제 취소 실패: " + errorBody));
                                    })
                    )
                    .bodyToMono(PaymentDtos.TossPaymentResponse.class)
                    .block();

            log.info("[결제취소 성공] MID={}, 주문번호={}, paymentKey={}, 취소금액={}",
                    response.mId(), response.orderId(), response.paymentKey(), response.totalAmount());
            return response;

        } catch (Exception e) {
            log.error("[결제취소 오류] paymentKey={}, 오류={}", paymentKey, e.getMessage(), e);
            throw new RuntimeException("결제 취소 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 결제 조회 API 호출
     *
     * @param paymentKey 결제 키
     * @return 결제 정보
     */
    public PaymentDtos.TossPaymentResponse getPayment(String paymentKey) {
        log.info("[결제조회 요청] paymentKey={}", paymentKey);

        try {
            PaymentDtos.TossPaymentResponse response = tossPaymentWebClient
                    .get()
                    .uri("/payments/{paymentKey}", paymentKey)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("[결제조회 실패] paymentKey={}, 에러={}", paymentKey, errorBody);
                                        return Mono.error(new RuntimeException("결제 조회 실패: " + errorBody));
                                    })
                    )
                    .bodyToMono(PaymentDtos.TossPaymentResponse.class)
                    .block();

            log.info("[결제조회 성공] MID={}, 주문번호={}, paymentKey={}, 상태={}",
                    response.mId(), response.orderId(), response.paymentKey(), response.status());
            return response;

        } catch (Exception e) {
            log.error("[결제조회 오류] paymentKey={}, 오류={}", paymentKey, e.getMessage(), e);
            throw new RuntimeException("결제 조회 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * orderId로 결제 조회 API 호출
     *
     * @param orderId 주문 ID
     * @return 결제 정보
     */
    public PaymentDtos.TossPaymentResponse getPaymentByOrderId(String orderId) {
        log.info("[주문번호로 결제조회 요청] 주문번호={}", orderId);

        try {
            PaymentDtos.TossPaymentResponse response = tossPaymentWebClient
                    .get()
                    .uri("/payments/orders/{orderId}", orderId)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("[주문번호로 결제조회 실패] 주문번호={}, 에러={}", orderId, errorBody);
                                        return Mono.error(new RuntimeException("결제 조회 실패: " + errorBody));
                                    })
                    )
                    .bodyToMono(PaymentDtos.TossPaymentResponse.class)
                    .block();

            log.info("[주문번호로 결제조회 성공] MID={}, 주문번호={}, paymentKey={}, 상태={}",
                    response.mId(), response.orderId(), response.paymentKey(), response.status());
            return response;

        } catch (Exception e) {
            log.error("[주문번호로 결제조회 오류] 주문번호={}, 오류={}", orderId, e.getMessage(), e);
            throw new RuntimeException("결제 조회 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
