package com.tigger.closetconnectproject.Market.Service;

import com.tigger.closetconnectproject.Market.Client.TossPaymentClient;
import com.tigger.closetconnectproject.Market.Dto.PaymentDtos;
import com.tigger.closetconnectproject.Market.Entity.Order;
import com.tigger.closetconnectproject.Market.Entity.OrderStatus;
import com.tigger.closetconnectproject.Market.Entity.ProductStatus;
import com.tigger.closetconnectproject.Market.Repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토스페이먼츠 결제 서비스
 * 결제 승인, 취소 등 토스 API 연동 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TossPaymentService {

    private final TossPaymentClient tossPaymentClient;
    private final OrderRepository orderRepository;
    private final MarketProductService productService;
    private final ChatService chatService;

    /**
     * 결제 승인 처리
     *
     * 1. 토스 API로 결제 승인 요청
     * 2. 주문 상태를 PAYMENT_PAID로 변경
     * 3. 상품 상태를 RESERVED로 변경
     * 4. 채팅방에 시스템 메시지 전송
     *
     * @param request 결제 승인 요청 (paymentKey, orderId, amount)
     * @return 결제 승인 응답
     */
    public PaymentDtos.ConfirmResponse confirmPayment(PaymentDtos.ConfirmRequest request) {
        log.info("[결제승인 시작] 주문번호={}, 금액={}", request.orderId(), request.amount());

        // 1. 주문 조회
        Order order = orderRepository.findByTossOrderId(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + request.orderId()));

        log.info("[주문정보] DB주문ID={}, 주문번호={}, 구매자ID={}, 판매자ID={}, 금액={}",
                order.getId(), order.getTossOrderId(), order.getBuyer().getUserId(),
                order.getSeller().getUserId(), order.getOrderAmount());

        // 2. 주문 상태 검증
        if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("결제 대기 상태가 아닙니다. 현재 상태: " + order.getOrderStatus().getKoreanName());
        }

        // 3. 금액 검증
        if (!order.getOrderAmount().equals(request.amount())) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다. 주문금액: " + order.getOrderAmount() + ", 요청금액: " + request.amount());
        }

        // 4. 토스 API 결제 승인 호출
        PaymentDtos.TossPaymentResponse tossResponse;
        try {
            tossResponse = tossPaymentClient.confirmPayment(request);
        } catch (Exception e) {
            log.error("[결제승인 실패] 주문번호={}, 오류={}", request.orderId(), e.getMessage(), e);
            throw new RuntimeException("결제 승인에 실패했습니다: " + e.getMessage(), e);
        }

        log.info("[토스응답 수신] MID={}, 주문번호={}, paymentKey={}, 결제수단={}, 금액={}",
                tossResponse.mId(), tossResponse.orderId(), tossResponse.paymentKey(),
                tossResponse.method(), tossResponse.totalAmount());

        // 5. 주문 정보 업데이트 (결제 승인)
        order.approvePayment(
                tossResponse.paymentKey(),
                tossResponse.getPaymentMethodEnum()
        );

        // 6. 상품 상태를 RESERVED로 변경
        productService.changeStatus(
                order.getProduct().getId(),
                order.getSeller().getUserId(),
                ProductStatus.RESERVED
        );

        // 7. 채팅방에 시스템 메시지 전송
        chatService.sendSystemMessage(
                order.getProduct().getId(),
                "구매자가 결제를 완료했습니다. 판매자는 상품을 발송해주세요."
        );

        log.info("[결제승인 완료] MID={}, DB주문ID={}, 주문번호={}, paymentKey={}",
                tossResponse.mId(), order.getId(), order.getTossOrderId(), order.getPaymentKey());

        return PaymentDtos.ConfirmResponse.builder()
                .orderId(order.getId())
                .tossOrderId(order.getTossOrderId())
                .paymentKey(order.getPaymentKey())
                .status(order.getOrderStatus().name())
                .amount(order.getOrderAmount())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().getKoreanName() : null)
                .approvedAt(order.getApprovedAt())
                .build();
    }

    /**
     * 결제 취소 처리
     *
     * @param orderId 주문 ID
     * @param userId 요청 사용자 ID (권한 확인용)
     * @param cancelReason 취소 사유
     * @return 취소 응답
     */
    public PaymentDtos.CancelResponse cancelPayment(Long orderId, Long userId, String cancelReason) {
        log.info("[결제취소 시작] DB주문ID={}, 사용자ID={}, 취소사유={}", orderId, userId, cancelReason);

        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        log.info("[주문정보] DB주문ID={}, 주문번호={}, paymentKey={}, 주문상태={}",
                order.getId(), order.getTossOrderId(), order.getPaymentKey(), order.getOrderStatus());

        // 2. 권한 확인 (구매자 또는 판매자만 취소 가능)
        if (!order.isParticipant(userId)) {
            throw new IllegalArgumentException("주문 참여자만 취소할 수 있습니다.");
        }

        // 3. 취소 가능 상태 확인
        if (!order.getOrderStatus().isCancellable()) {
            throw new IllegalStateException("취소 불가능한 상태입니다. 현재 상태: " + order.getOrderStatus().getKoreanName());
        }

        // 4. 결제 승인된 경우 토스 API로 취소 요청
        if (order.getPaymentKey() != null) {
            try {
                PaymentDtos.TossPaymentResponse cancelResponse = tossPaymentClient.cancelPayment(
                        order.getPaymentKey(), cancelReason);
                log.info("[토스취소 응답] MID={}, 주문번호={}, paymentKey={}",
                        cancelResponse.mId(), cancelResponse.orderId(), cancelResponse.paymentKey());
            } catch (Exception e) {
                log.error("[결제취소 실패] DB주문ID={}, 주문번호={}, 오류={}",
                        orderId, order.getTossOrderId(), e.getMessage(), e);
                throw new RuntimeException("결제 취소에 실패했습니다: " + e.getMessage(), e);
            }
            order.refund(cancelReason);
        } else {
            // 결제 전 취소
            log.info("[결제전 주문취소] 주문번호={}", order.getTossOrderId());
            order.cancel(cancelReason);
        }

        // 5. 상품 상태를 ON_SALE로 복원
        productService.changeStatus(
                order.getProduct().getId(),
                order.getSeller().getUserId(),
                ProductStatus.ON_SALE
        );

        // 6. 채팅방에 시스템 메시지 전송
        chatService.sendSystemMessage(
                order.getProduct().getId(),
                "주문이 취소되었습니다. 사유: " + cancelReason
        );

        log.info("[결제취소 완료] DB주문ID={}, 주문번호={}", orderId, order.getTossOrderId());

        return PaymentDtos.CancelResponse.builder()
                .paymentKey(order.getPaymentKey())
                .orderId(order.getTossOrderId())
                .status(order.getOrderStatus().name())
                .cancelReason(cancelReason)
                .canceledAt(order.getRefundedAt())
                .build();
    }
}
