package com.tigger.closetconnectproject.Market.Repository;

import com.tigger.closetconnectproject.Market.Entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 채팅 메시지 Repository
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 채팅방의 메시지 목록 조회 (페이징)
     */
    Page<ChatMessage> findByChatRoom_IdOrderByCreatedAtAsc(Long chatRoomId, Pageable pageable);

    /**
     * 채팅방의 메시지 목록 조회 (전체)
     */
    List<ChatMessage> findByChatRoom_IdOrderByCreatedAtAsc(Long chatRoomId);

    /**
     * 채팅방의 읽지 않은 메시지 개수
     */
    @Query("""
        SELECT COUNT(m) FROM ChatMessage m
        WHERE m.chatRoom.id = :chatRoomId
          AND m.sender.userId != :userId
          AND m.isRead = false
        """)
    long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    /**
     * 채팅방의 메시지 모두 읽음 처리
     */
    @Modifying
    @Query("""
        UPDATE ChatMessage m
        SET m.isRead = true
        WHERE m.chatRoom.id = :chatRoomId
          AND m.sender.userId != :userId
          AND m.isRead = false
        """)
    void markAllAsRead(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
}
