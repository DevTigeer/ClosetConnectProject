package com.tigger.closetconnectproject.Market.Entity;

import com.tigger.closetconnectproject.User.Entity.Users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

/**
 * 채팅 메시지 엔티티
 */
@Entity
@Table(name = "chat_message",
        indexes = {
            @Index(name = "idx_message_room_created", columnList = "chat_room_id, created_at"),
            @Index(name = "idx_message_sender", columnList = "sender_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 채팅방
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    // 발신자 (시스템 메시지인 경우 null 가능)
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "sender_id")
    private Users sender;

    // 메시지 타입
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType messageType;

    // 메시지 내용
    @Lob
    @Column(nullable = false)
    private String content;

    // 읽음 여부 (옵션)
    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    // 생성 시간
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 메시지 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
    }
}
