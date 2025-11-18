package com.tigger.closetconnectproject.Market.Controller;

import com.tigger.closetconnectproject.Market.Dto.ChatDtos;
import com.tigger.closetconnectproject.Market.Service.ChatService;
import com.tigger.closetconnectproject.Security.AppUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * 채팅 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/market/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅방 생성 또는 기존 채팅방 조회
     * POST /api/v1/market/chat/rooms
     */
    @PostMapping("/rooms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatDtos.ChatRoomRes> createChatRoom(
            @Valid @RequestBody ChatDtos.CreateRoomReq req,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long buyerId = principal.getUser().getUserId();
        ChatDtos.ChatRoomRes response = chatService.createOrGetChatRoom(req.getProductId(), buyerId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 채팅방 목록 조회
     * GET /api/v1/market/chat/rooms
     */
    @GetMapping("/rooms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatDtos.ChatRoomRes>> getMyChatRooms(
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long userId = principal.getUser().getUserId();
        List<ChatDtos.ChatRoomRes> response = chatService.getMyChatRooms(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 채팅방 메시지 목록 조회
     * GET /api/v1/market/chat/rooms/{roomId}/messages
     */
    @GetMapping("/rooms/{roomId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatDtos.ChatMessageRes>> getChatMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long userId = principal.getUser().getUserId();
        List<ChatDtos.ChatMessageRes> response = chatService.getChatMessages(roomId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 메시지 읽음 처리
     * POST /api/v1/market/chat/rooms/{roomId}/read
     */
    @PostMapping("/rooms/{roomId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long roomId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long userId = principal.getUser().getUserId();
        chatService.markMessagesAsRead(roomId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 직접 사용자 간 채팅방 생성 또는 조회 (상품과 무관)
     * POST /api/v1/market/chat/direct
     */
    @PostMapping("/direct")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatDtos.ChatRoomRes> createDirectChat(
            @RequestParam Long targetUserId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long currentUserId = principal.getUser().getUserId();
        ChatDtos.ChatRoomRes response = chatService.createOrGetDirectChat(currentUserId, targetUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * WebSocket 메시지 수신 및 전송
     * 클라이언트 → /app/chat.send
     * 서버 → /queue/chat/{roomId}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatDtos.SendMessageReq req, Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        // Principal에서 AppUserDetails 추출
        AppUserDetails userDetails = null;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
            if (auth.getPrincipal() instanceof AppUserDetails) {
                userDetails = (AppUserDetails) auth.getPrincipal();
            }
        }

        if (userDetails == null || userDetails.getUser() == null) {
            throw new IllegalStateException("인증 정보를 찾을 수 없습니다.");
        }

        Long senderId = userDetails.getUser().getUserId();

        // 메시지 저장
        ChatDtos.ChatMessageRes message = chatService.sendMessage(req.getRoomId(), senderId, req);

        // 채팅방 구독자들에게 메시지 전송
        messagingTemplate.convertAndSend("/queue/chat/" + req.getRoomId(), message);
    }
}
