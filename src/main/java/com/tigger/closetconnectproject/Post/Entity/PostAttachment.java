package com.tigger.closetconnectproject.Post.Entity;

import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "community_post_attachment")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor @Builder
public class PostAttachment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /** 퍼블릭 접근 가능한 URL (ex. /uploads/...) */
    @Column(nullable = false, length = 500)
    private String url;

    /** 스토리지 내부 키 (ex. userId/yyyy/mm/dd/uuid.ext) */
    @Column(nullable = false, length = 500)
    private String imageKey;

    /** 원본 파일명/컨텐츠타입/사이즈는 요청에서 추출 */
    @Column(nullable = false, length = 255)
    private String filename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long size;
}
