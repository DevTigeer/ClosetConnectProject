package com.tigger.closetconnectproject.Post.Entity;

import com.tigger.closetconnectproject.Common.Entity.BaseTimeEntity;
import com.tigger.closetconnectproject.User.Entity.Users;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "community_comment",
        indexes = @Index(name="idx_comment_post_created", columnList = "post_id, created_at"))
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor @Builder
public class Comment extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Users author;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Lob @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private CommentStatus status;

    public void edit(String content) { if (content != null) this.content = content; }
    public void softDelete() { this.status = CommentStatus.DELETED; }
}
