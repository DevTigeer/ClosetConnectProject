package com.tigger.closetconnectproject.Market.Entity;

import com.tigger.closetconnectproject.Common.Entity.BaseTimeEntity;
import com.tigger.closetconnectproject.User.Entity.Users;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;

/**
 * 채팅방 엔티티
 * 1) 상품별 판매자-구매자 1:1 채팅방 (marketProduct != null)
 * 2) 직접 사용자 간 1:1 채팅방 (marketProduct == null)
 */
@Entity
@Table(name = "chat_room",
        indexes = {
            @Index(name = "idx_chatroom_product", columnList = "market_product_id"),
            @Index(name = "idx_chatroom_buyer", columnList = "buyer_id"),
            @Index(name = "idx_chatroom_seller", columnList = "seller_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상품 (선택적 - 직접 채팅의 경우 null)
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "market_product_id")
    private MarketProduct marketProduct;

    // 판매자
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Users seller;

    // 구매자
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Users buyer;

    // 마지막 메시지 내용 (미리보기용)
    @Column(length = 500)
    private String lastMessage;

    // 마지막 메시지 시간
    @Column
    private java.time.LocalDateTime lastMessageAt;

    /**
     * 마지막 메시지 업데이트
     */
    public void updateLastMessage(String message, java.time.LocalDateTime timestamp) {
        this.lastMessage = message;
        this.lastMessageAt = timestamp;
    }

    /**
     * 특정 사용자가 이 채팅방의 참여자인지 확인
     */
    public boolean isParticipant(Long userId) {
        return seller.getUserId().equals(userId) || buyer.getUserId().equals(userId);
    }

    /**
     * 상대방 사용자 ID 가져오기
     */
    public Long getOtherUserId(Long myUserId) {
        if (seller.getUserId().equals(myUserId)) {
            return buyer.getUserId();
        } else if (buyer.getUserId().equals(myUserId)) {
            return seller.getUserId();
        }
        return null;
    }

    /**
     * 상대방 닉네임 가져오기
     */
    public String getOtherUserNickname(Long myUserId) {
        if (seller.getUserId().equals(myUserId)) {
            return buyer.getNickname();
        } else if (buyer.getUserId().equals(myUserId)) {
            return seller.getNickname();
        }
        return null;
    }

    /**
     * 상품 기반 채팅방인지 확인
     */
    public boolean isProductChat() {
        return marketProduct != null;
    }

    /**
     * 직접 사용자 간 채팅방인지 확인
     */
    public boolean isDirectChat() {
        return marketProduct == null;
    }
}
