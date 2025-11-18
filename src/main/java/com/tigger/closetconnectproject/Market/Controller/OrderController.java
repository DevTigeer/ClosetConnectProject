package com.tigger.closetconnectproject.Market.Controller;

import com.tigger.closetconnectproject.Security.AppUserDetails;
import com.tigger.closetconnectproject.Market.Dto.OrderDtos;
import com.tigger.closetconnectproject.Market.Service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 컨트롤러
 * 주문 생성, 조회, 발송, 구매확정 API
 */
@RestController
@RequestMapping("/api/v1/market/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성 (결제 전)
     *
     * POST /api/v1/market/orders
     * {
     *   "productId": 1
     * }
     *
     * @param request 주문 생성 요청
     * @param userDetails 인증된 사용자
     * @return 주문 생성 응답 (토스 결제창 호출용 데이터)
     */
    @PostMapping
    public ResponseEntity<OrderDtos.CreateResponse> createOrder(
            @Valid @RequestBody OrderDtos.CreateRequest request,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        log.info("주문 생성 API 호출: userId={}, productId={}", userDetails.getUser().getUserId(), request.productId());

        OrderDtos.CreateResponse response = orderService.createOrder(
                userDetails.getUser().getUserId(),
                request
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 주문 상세 조회
     *
     * GET /api/v1/market/orders/{orderId}
     *
     * @param orderId 주문 ID
     * @param userDetails 인증된 사용자
     * @return 주문 상세 정보
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDtos.DetailResponse> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        log.info("주문 상세 조회 API 호출: orderId={}, userId={}", orderId, userDetails.getUser().getUserId());

        OrderDtos.DetailResponse response = orderService.getOrder(orderId, userDetails.getUser().getUserId());

        return ResponseEntity.ok(response);
    }

    /**
     * 내 구매 주문 목록 조회
     *
     * GET /api/v1/market/orders/buyer?page=0&size=20
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param userDetails 인증된 사용자
     * @return 구매 주문 목록
     */
    @GetMapping("/buyer")
    public ResponseEntity<Page<OrderDtos.ListResponse>> getBuyerOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        log.info("구매 주문 목록 조회 API 호출: userId={}", userDetails.getUser().getUserId());

        Page<OrderDtos.ListResponse> response = orderService.getBuyerOrders(
                userDetails.getUser().getUserId(),
                page,
                size
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 내 판매 주문 목록 조회
     *
     * GET /api/v1/market/orders/seller?page=0&size=20
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param userDetails 인증된 사용자
     * @return 판매 주문 목록
     */
    @GetMapping("/seller")
    public ResponseEntity<Page<OrderDtos.ListResponse>> getSellerOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        log.info("판매 주문 목록 조회 API 호출: userId={}", userDetails.getUser().getUserId());

        Page<OrderDtos.ListResponse> response = orderService.getSellerOrders(
                userDetails.getUser().getUserId(),
                page,
                size
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 발송 처리 (판매자)
     *
     * POST /api/v1/market/orders/{orderId}/ship
     * {
     *   "shippingCompany": "CJ대한통운",
     *   "trackingNumber": "123456789"
     * }
     *
     * @param orderId 주문 ID
     * @param request 발송 정보
     * @param userDetails 인증된 사용자
     * @return 발송 응답
     */
    @PostMapping("/{orderId}/ship")
    public ResponseEntity<OrderDtos.ShipResponse> shipOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderDtos.ShipRequest request,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        log.info("발송 처리 API 호출: orderId={}, sellerId={}", orderId, userDetails.getUser().getUserId());

        OrderDtos.ShipResponse response = orderService.shipOrder(
                orderId,
                userDetails.getUser().getUserId(),
                request
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 구매 확정 (구매자)
     *
     * POST /api/v1/market/orders/{orderId}/confirm
     *
     * @param orderId 주문 ID
     * @param userDetails 인증된 사용자
     * @return 구매 확정 응답
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderDtos.ConfirmResponse> confirmOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        log.info("구매 확정 API 호출: orderId={}, buyerId={}", orderId, userDetails.getUser().getUserId());

        OrderDtos.ConfirmResponse response = orderService.confirmOrder(
                orderId,
                userDetails.getUser().getUserId()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 주문 취소 (결제 전)
     *
     * DELETE /api/v1/market/orders/{orderId}
     * {
     *   "cancelReason": "구매 취소"
     * }
     *
     * @param orderId 주문 ID
     * @param request 취소 요청
     * @param userDetails 인증된 사용자
     * @return 취소 응답
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderDtos.CancelResponse> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderDtos.CancelRequest request,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        log.info("주문 취소 API 호출: orderId={}, userId={}", orderId, userDetails.getUser().getUserId());

        OrderDtos.CancelResponse response = orderService.cancelOrder(
                orderId,
                userDetails.getUser().getUserId(),
                request.cancelReason()
        );

        return ResponseEntity.ok(response);
    }
}
