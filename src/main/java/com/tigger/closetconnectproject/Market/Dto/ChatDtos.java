package com.tigger.closetconnectproject.Market.Dto;

import com.tigger.closetconnectproject.Market.Entity.ChatMessage;
import com.tigger.closetconnectproject.Market.Entity.ChatRoom;
import com.tigger.closetconnectproject.Market.Entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅 DTO 모음
 */
public class ChatDtos {

    /**
     * 채팅방 생성/조회 요청
     */
    @Getter @Setter @NoArgsConstructor
    public static class CreateRoomReq {
        @NotNull
        private Long productId;  // 상품 ID
    }

    /**
     * 메시지 전송 요청 (WebSocket)
     */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SendMessageReq {
        @NotNull
        private Long roomId;

        @NotNull
        private MessageType messageType;

        @NotBlank
        private String content;
    }

    /**
     * 채팅방 목록 응답
     */
    @Getter @Builder
    public static class ChatRoomRes {
        private Long roomId;
        private Long productId;
        private String productTitle;
        private String productThumbnail;
        private String otherUserNickname;  // 상대방 닉네임
        private Long otherUserId;
        private String lastMessage;
        private LocalDateTime lastMessageAt;
        private long unreadCount;

        public static ChatRoomRes of(ChatRoom room, Long myUserId, String thumbnail, long unreadCount) {
            return ChatRoomRes.builder()
                    .roomId(room.getId())
                    .productId(room.getMarketProduct() != null ? room.getMarketProduct().getId() : null)
                    .productTitle(room.getMarketProduct() != null ? room.getMarketProduct().getTitle() : null)
                    .productThumbnail(thumbnail)
                    .otherUserNickname(room.getOtherUserNickname(myUserId))
                    .otherUserId(room.getOtherUserId(myUserId))
                    .lastMessage(room.getLastMessage())
                    .lastMessageAt(room.getLastMessageAt())
                    .unreadCount(unreadCount)
                    .build();
        }
    }

    /**
     * 채팅 메시지 응답
     */
    @Getter @Builder
    public static class ChatMessageRes {
        private Long messageId;
        private Long roomId;
        private Long senderId;
        private String senderNickname;
        private MessageType messageType;
        private String content;
        private Boolean isRead;
        private LocalDateTime createdAt;

        public static ChatMessageRes of(ChatMessage message) {
            return ChatMessageRes.builder()
                    .messageId(message.getId())
                    .roomId(message.getChatRoom().getId())
                    .senderId(message.getSender() != null ? message.getSender().getUserId() : null)
                    .senderNickname(message.getSender() != null ? message.getSender().getNickname() : "시스템")
                    .messageType(message.getMessageType())
                    .content(message.getContent())
                    .isRead(message.getIsRead())
                    .createdAt(message.getCreatedAt())
                    .build();
        }
    }
}
