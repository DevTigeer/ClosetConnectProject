package com.tigger.closetconnectproject.Market.Controller;

import com.tigger.closetconnectproject.Market.Config.TossPaymentProperties;
import com.tigger.closetconnectproject.Security.AppUserDetails;
import com.tigger.closetconnectproject.Market.Dto.PaymentDtos;
import com.tigger.closetconnectproject.Market.Service.TossPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 결제 컨트롤러
 * 토스페이먼츠 결제 승인, 취소 API
 */
@RestController
@RequestMapping("/api/v1/market/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final TossPaymentService paymentService;
    private final TossPaymentProperties properties;

    /**
     * 결제 승인
     *
     * 토스 결제창에서 결제 완료 후 클라이언트가 이 API를 호출
     *
     * POST /api/v1/market/payments/confirm
     * {
     *   "paymentKey": "...",
     *   "orderId": "...",
     *   "amount": 25000
     * }
     *
     * @param request 결제 승인 요청
     * @return 결제 승인 응답
     */
    @PostMapping("/confirm")
    public ResponseEntity<PaymentDtos.ConfirmResponse> confirmPayment(
            @Valid @RequestBody PaymentDtos.ConfirmRequest request
    ) {
        log.info("==================== 결제 승인 API 호출 ====================");
        log.info("[요청정보] 주문번호={}, 금액={}, paymentKey={}",
                request.orderId(), request.amount(), request.paymentKey());
        log.info("[설정확인] 클라이언트키={}", properties.getClientKey());
        log.info("[설정확인] 시크릿키={}", properties.getSecretKey());
        log.info("[설정확인] API URL={}", properties.getApiUrl());
        log.info("[설정확인] 테스트모드={}", properties.isTestMode());
        log.info("=======================================================");

        PaymentDtos.ConfirmResponse response = paymentService.confirmPayment(request);

        return ResponseEntity.ok(response);
    }

    /**
     * 결제 취소
     *
     * DELETE /api/v1/market/payments/{orderId}
     * {
     *   "cancelReason": "구매자 변심"
     * }
     *
     * @param orderId 주문 ID
     * @param request 취소 요청
     * @param userDetails 인증된 사용자
     * @return 취소 응답
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<PaymentDtos.CancelResponse> cancelPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentDtos.CancelRequest request,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        log.info("결제 취소 API 호출: orderId={}, userId={}", orderId, userDetails.getUser().getUserId());

        PaymentDtos.CancelResponse response = paymentService.cancelPayment(
                orderId,
                userDetails.getUser().getUserId(),
                request.cancelReason()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 결제 실패 웹훅 (선택 사항)
     *
     * 토스페이먼츠에서 결제 실패 시 호출하는 웹훅 엔드포인트
     * 1차 버전에서는 프론트엔드에서 failUrl로 리다이렉트되므로 생략 가능
     */
    @PostMapping("/webhook/fail")
    public ResponseEntity<Void> paymentFailWebhook(@RequestBody String payload) {
        log.warn("결제 실패 웹훅 수신: {}", payload);
        // TODO: 필요 시 실패 로깅 또는 알림 처리
        return ResponseEntity.ok().build();
    }
}
