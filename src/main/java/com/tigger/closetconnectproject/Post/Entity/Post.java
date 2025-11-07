package com.tigger.closetconnectproject.Post.Entity;

import com.tigger.closetconnectproject.Common.Entity.BaseTimeEntity;
import com.tigger.closetconnectproject.Community.Entity.CommunityBoard;
import com.tigger.closetconnectproject.User.Entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "community_post",
        indexes = {
                @Index(name = "idx_post_board_created", columnList = "board_id, created_at"),
                @Index(name = "idx_post_visibility", columnList = "visibility"),
                @Index(name = "idx_post_status", columnList = "status")
        })
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor @Builder
public class Post extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private CommunityBoard board;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Users author;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Visibility visibility;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PostStatus status;

    @Column(nullable = false)
    private boolean pinned;

    @Column(nullable = false)
    private long viewCount;

    @Column(nullable = false)
    private long likeCount;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostAttachment> attachments = new ArrayList<>();

    /* domain */
    public void edit(String title, String content, Visibility visibility) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (visibility != null) this.visibility = visibility;
    }
    public void increaseView() { this.viewCount += 1; }
    public void incLike() { this.likeCount += 1; }
    public void decLike() { this.likeCount = Math.max(0, this.likeCount - 1); }
    public void pin() { this.pinned = true; }
    public void unpin() { this.pinned = false; }
    public void hide() { this.status = PostStatus.HIDDEN; }
    public void blind() { this.status = PostStatus.BLINDED; }
    public void softDelete() { this.status = PostStatus.DELETED; }
    public void moveToBoard(CommunityBoard newBoard) { this.board = newBoard; }
    public void restore() {
        this.status = PostStatus.NORMAL;
    }

}
