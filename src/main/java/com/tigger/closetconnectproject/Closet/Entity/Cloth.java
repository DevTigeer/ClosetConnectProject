package com.tigger.closetconnectproject.Closet.Entity;

import com.tigger.closetconnectproject.User.Entity.Users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "cloth")
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Cloth {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 512)
    private String imageUrl;  // 기존 호환성 유지 (deprecated 예정)

    @Column(name = "original_image_url", length = 512)
    private String originalImageUrl;  // 원본 이미지 URL

    @Column(name = "removed_bg_image_url", length = 512)
    private String removedBgImageUrl;  // 배경 제거된 이미지 URL

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
