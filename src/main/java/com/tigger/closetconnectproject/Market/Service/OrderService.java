package com.tigger.closetconnectproject.Market.Service;

import com.tigger.closetconnectproject.Market.Dto.OrderDtos;
import com.tigger.closetconnectproject.Market.Entity.*;
import com.tigger.closetconnectproject.Market.Repository.MarketProductImageRepository;
import com.tigger.closetconnectproject.Market.Repository.OrderRepository;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 주문 서비스
 * 주문 생성, 조회, 발송, 구매확정 등 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UsersRepository usersRepository;
    private final MarketProductService productService;
    private final MarketProductImageRepository imageRepository;
    private final ChatService chatService;

    /**
     * 주문 생성
     *
     * @param buyerId 구매자 ID
     * @param request 주문 생성 요청
     * @return 주문 생성 응답 (토스 결제창 호출에 필요한 정보)
     */
    public OrderDtos.CreateResponse createOrder(Long buyerId, OrderDtos.CreateRequest request) {
        // 1. 구매자 조회
        Users buyer = usersRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("구매자를 찾을 수 없습니다."));

        // 2. 상품 조회
        MarketProduct product = productService.getProductEntity(request.productId());

        // 3. 판매자 조회
        Users seller = product.getSeller();

        // 4. 본인 상품 구매 방지
        if (seller.getUserId().equals(buyerId)) {
            throw new IllegalArgumentException("본인의 상품은 구매할 수 없습니다.");
        }

        // 5. 상품 상태 확인 (판매중인 상품만 구매 가능)
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new IllegalStateException("판매중인 상품이 아닙니다. 현재 상태: " + product.getStatus());
        }

        // 6. 해당 상품에 대한 진행 중인 주문이 있는지 확인
        boolean hasActiveOrder = orderRepository.existsByProduct_IdAndOrderStatusIn(
                request.productId(),
                OrderStatus.PAYMENT_PENDING,
                OrderStatus.PAYMENT_PAID,
                OrderStatus.SHIPPED
        );
        if (hasActiveOrder) {
            throw new IllegalStateException("해당 상품에 대한 진행 중인 주문이 이미 존재합니다.");
        }

        // 7. 토스 주문 ID 생성 (UUID)
        String tossOrderId = UUID.randomUUID().toString();

        // 8. 주문 생성
        Order order = Order.builder()
                .tossOrderId(tossOrderId)
                .buyer(buyer)
                .seller(seller)
                .product(product)
                .orderAmount(product.getPrice())
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .build();

        orderRepository.save(order);

        return OrderDtos.CreateResponse.builder()
                .orderId(order.getId())
                .tossOrderId(tossOrderId)
                .amount(product.getPrice())
                .orderName(product.getTitle())
                .customerName(buyer.getNickname())
                .build();
    }

    /**
     * 주문 상세 조회
     */
    @Transactional(readOnly = true)
    public OrderDtos.DetailResponse getOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        // 권한 확인 (주문 참여자만 조회 가능)
        if (!order.isParticipant(userId)) {
            throw new IllegalArgumentException("주문 참여자만 조회할 수 있습니다.");
        }

        // 상품 썸네일 조회
        String thumbnail = getProductThumbnail(order.getProduct().getId());

        return OrderDtos.DetailResponse.from(order, thumbnail);
    }

    /**
     * 구매자 주문 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<OrderDtos.ListResponse> getBuyerOrders(Long buyerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderRepository.findByBuyer_UserId(buyerId, pageable);
        return orders.map(order -> {
            String thumbnail = getProductThumbnail(order.getProduct().getId());
            return OrderDtos.ListResponse.from(order, thumbnail);
        });
    }

    /**
     * 판매자 주문 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<OrderDtos.ListResponse> getSellerOrders(Long sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderRepository.findBySeller_UserId(sellerId, pageable);
        return orders.map(order -> {
            String thumbnail = getProductThumbnail(order.getProduct().getId());
            return OrderDtos.ListResponse.from(order, thumbnail);
        });
    }

    /**
     * 상품 썸네일 조회 (첫 번째 이미지)
     */
    private String getProductThumbnail(Long productId) {
        return imageRepository.findByMarketProduct_IdOrderByOrderIndexAsc(productId)
                .stream()
                .findFirst()
                .map(MarketProductImage::getImageUrl)
                .orElse(null);
    }

    /**
     * 발송 처리 (판매자)
     *
     * @param orderId 주문 ID
     * @param sellerId 판매자 ID
     * @param request 발송 정보
     * @return 발송 응답
     */
    public OrderDtos.ShipResponse shipOrder(Long orderId, Long sellerId, OrderDtos.ShipRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        // 권한 확인 (판매자만)
        if (!order.isSeller(sellerId)) {
            throw new IllegalArgumentException("판매자만 발송 처리할 수 있습니다.");
        }

        // 발송 처리
        order.ship(request.shippingCompany(), request.trackingNumber());

        // 채팅방에 시스템 메시지
        chatService.sendSystemMessage(
                order.getProduct().getId(),
                "판매자가 상품을 발송했습니다. 운송장번호: " + request.trackingNumber()
        );

        return OrderDtos.ShipResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus())
                .shippingCompany(order.getShippingCompany())
                .trackingNumber(order.getTrackingNumber())
                .shippedAt(order.getShippedAt())
                .build();
    }

    /**
     * 구매 확정 (구매자)
     *
     * @param orderId 주문 ID
     * @param buyerId 구매자 ID
     * @return 구매 확정 응답
     */
    public OrderDtos.ConfirmResponse confirmOrder(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        // 권한 확인 (구매자만)
        if (!order.isBuyer(buyerId)) {
            throw new IllegalArgumentException("구매자만 구매 확정할 수 있습니다.");
        }

        // 구매 확정
        order.confirm();

        // 상품 상태를 SOLD로 변경
        productService.changeStatus(
                order.getProduct().getId(),
                order.getSeller().getUserId(),
                ProductStatus.SOLD
        );

        // 자동 정산 처리 (1차 버전에서는 즉시 정산)
        order.settle();

        // 채팅방에 시스템 메시지
        chatService.sendSystemMessage(
                order.getProduct().getId(),
                "구매자가 구매를 확정했습니다. 거래가 완료되었습니다."
        );

        return OrderDtos.ConfirmResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus())
                .confirmedAt(order.getConfirmedAt())
                .build();
    }

    /**
     * 주문 취소 (결제 전)
     */
    public OrderDtos.CancelResponse cancelOrder(Long orderId, Long userId, String cancelReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        // 권한 확인
        if (!order.isParticipant(userId)) {
            throw new IllegalArgumentException("주문 참여자만 취소할 수 있습니다.");
        }

        // 취소 처리
        order.cancel(cancelReason);

        return OrderDtos.CancelResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus())
                .cancelReason(cancelReason)
                .build();
    }
}
