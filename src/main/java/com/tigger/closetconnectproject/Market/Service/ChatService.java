package com.tigger.closetconnectproject.Market.Service;

import com.tigger.closetconnectproject.Market.Dto.ChatDtos;
import com.tigger.closetconnectproject.Market.Entity.*;
import com.tigger.closetconnectproject.Market.Repository.*;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 채팅 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepo;
    private final ChatMessageRepository chatMessageRepo;
    private final MarketProductRepository productRepo;
    private final MarketProductImageRepository imageRepo;
    private final UsersRepository userRepo;

    /**
     * 채팅방 생성 또는 기존 채팅방 조회
     */
    public ChatDtos.ChatRoomRes createOrGetChatRoom(Long productId, Long buyerId) {
        // 상품 조회
        MarketProduct product = productRepo.findByIdWithDetails(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 판매자 본인은 채팅방 생성 불가
        if (product.getSeller().getUserId().equals(buyerId)) {
            throw new IllegalArgumentException("본인 상품에는 채팅을 시작할 수 없습니다.");
        }

        // 구매자 조회
        Users buyer = userRepo.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 기존 채팅방 확인
        ChatRoom chatRoom = chatRoomRepo.findByMarketProduct_IdAndBuyer_UserId(productId, buyerId)
                .orElseGet(() -> {
                    // 새 채팅방 생성
                    ChatRoom newRoom = ChatRoom.builder()
                            .marketProduct(product)
                            .seller(product.getSeller())
                            .buyer(buyer)
                            .build();
                    return chatRoomRepo.save(newRoom);
                });

        // 썸네일 조회
        String thumbnail = getThumbnail(productId);

        // 읽지 않은 메시지 개수
        long unreadCount = chatMessageRepo.countUnreadMessages(chatRoom.getId(), buyerId);

        return ChatDtos.ChatRoomRes.of(chatRoom, buyerId, thumbnail, unreadCount);
    }

    /**
     * 내 채팅방 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChatDtos.ChatRoomRes> getMyChatRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomRepo.findByUserIdWithDetails(userId);

        // 각 채팅방의 썸네일 조회
        Map<Long, String> thumbnailMap = new HashMap<>();
        for (ChatRoom room : rooms) {
            String thumbnail = room.getMarketProduct() != null
                    ? getThumbnail(room.getMarketProduct().getId())
                    : null;  // 직접 채팅은 썸네일 없음
            thumbnailMap.put(room.getId(), thumbnail);
        }

        // 읽지 않은 메시지 개수 조회
        Map<Long, Long> unreadCountMap = new HashMap<>();
        for (ChatRoom room : rooms) {
            long count = chatMessageRepo.countUnreadMessages(room.getId(), userId);
            unreadCountMap.put(room.getId(), count);
        }

        return rooms.stream()
                .map(room -> ChatDtos.ChatRoomRes.of(
                        room,
                        userId,
                        thumbnailMap.get(room.getId()),
                        unreadCountMap.getOrDefault(room.getId(), 0L)
                ))
                .collect(Collectors.toList());
    }

    /**
     * 채팅방 메시지 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChatDtos.ChatMessageRes> getChatMessages(Long roomId, Long userId) {
        // 채팅방 조회 및 권한 확인
        ChatRoom room = chatRoomRepo.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        if (!room.isParticipant(userId)) {
            throw new AccessDeniedException("채팅방에 접근할 권한이 없습니다.");
        }

        // 메시지 조회
        List<ChatMessage> messages = chatMessageRepo.findByChatRoom_IdOrderByCreatedAtAsc(roomId);

        return messages.stream()
                .map(ChatDtos.ChatMessageRes::of)
                .collect(Collectors.toList());
    }

    /**
     * 메시지 전송
     */
    public ChatDtos.ChatMessageRes sendMessage(Long roomId, Long senderId, ChatDtos.SendMessageReq req) {
        // 채팅방 조회 및 권한 확인
        ChatRoom room = chatRoomRepo.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        if (!room.isParticipant(senderId)) {
            throw new AccessDeniedException("채팅방에 접근할 권한이 없습니다.");
        }

        // 발신자 조회
        Users sender = userRepo.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 메시지 생성
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .messageType(req.getMessageType())
                .content(req.getContent())
                .isRead(false)
                .build();

        chatMessageRepo.save(message);

        // 채팅방 마지막 메시지 업데이트
        room.updateLastMessage(
                req.getMessageType() == MessageType.TEXT ? req.getContent() : "[이미지]",
                LocalDateTime.now()
        );

        return ChatDtos.ChatMessageRes.of(message);
    }

    /**
     * 메시지 읽음 처리
     */
    public void markMessagesAsRead(Long roomId, Long userId) {
        // 채팅방 조회 및 권한 확인
        ChatRoom room = chatRoomRepo.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        if (!room.isParticipant(userId)) {
            throw new AccessDeniedException("채팅방에 접근할 권한이 없습니다.");
        }

        // 모든 메시지 읽음 처리
        chatMessageRepo.markAllAsRead(roomId, userId);
    }

    /**
     * 시스템 메시지 전송 (거래 상태 변경 등)
     */
    public void sendSystemMessage(Long productId, String content) {
        // 상품의 모든 채팅방에 시스템 메시지 전송
        List<ChatRoom> rooms = chatRoomRepo.findByProductIdWithBuyer(productId);

        for (ChatRoom room : rooms) {
            ChatMessage systemMessage = ChatMessage.builder()
                    .chatRoom(room)
                    .sender(null)  // 시스템 메시지
                    .messageType(MessageType.SYSTEM)
                    .content(content)
                    .isRead(false)
                    .build();

            chatMessageRepo.save(systemMessage);

            // 마지막 메시지 업데이트
            room.updateLastMessage(content, LocalDateTime.now());
        }
    }

    /**
     * 직접 사용자 간 채팅방 생성 또는 조회 (상품과 무관)
     */
    public ChatDtos.ChatRoomRes createOrGetDirectChat(Long currentUserId, Long targetUserId) {
        // 자기 자신과는 채팅 불가
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("자기 자신과는 채팅할 수 없습니다.");
        }

        // 사용자 조회
        Users currentUser = userRepo.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Users targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다."));

        // 기존 채팅방 확인
        ChatRoom chatRoom = chatRoomRepo.findDirectChatBetweenUsers(currentUserId, targetUserId)
                .orElseGet(() -> {
                    // 새 채팅방 생성 (상품 없이, 두 사용자만)
                    ChatRoom newRoom = ChatRoom.builder()
                            .marketProduct(null)  // 직접 채팅은 상품이 없음
                            .seller(currentUser)   // 이 경우 seller/buyer는 단순히 두 사용자를 의미
                            .buyer(targetUser)
                            .build();
                    return chatRoomRepo.save(newRoom);
                });

        // 읽지 않은 메시지 개수
        long unreadCount = chatMessageRepo.countUnreadMessages(chatRoom.getId(), currentUserId);

        return ChatDtos.ChatRoomRes.of(chatRoom, currentUserId, null, unreadCount);
    }

    /**
     * 상품 썸네일 조회
     */
    private String getThumbnail(Long productId) {
        if (productId == null) return null;  // 직접 채팅의 경우
        List<MarketProductImage> images = imageRepo.findByMarketProduct_IdOrderByOrderIndexAsc(productId);
        return images.isEmpty() ? null : images.get(0).getImageUrl();
    }
}
