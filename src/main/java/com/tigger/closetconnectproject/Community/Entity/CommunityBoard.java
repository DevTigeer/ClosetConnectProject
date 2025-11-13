package com.tigger.closetconnectproject.Community.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "community_board")
public class CommunityBoard {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=50)
    private String name;

    @Column(nullable=false, length=50, unique = true)
    private String slug; // free, item, promo, ootd, select, market

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=16)
    private BoardType type; // FREE, ITEM, PROMO, OOTD, SELECT, MARKET

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=16)
    private Visibility visibility; // PUBLIC, PRIVATE, HIDDEN, ARCHIVED

    @Column(nullable=false)
    private boolean isSystem; // 기본 보드인지

    @Column(nullable=false)
    private Integer sortOrder;

    @Column(nullable=false)
    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private LocalDateTime deletedAt;

    @PrePersist
    void prePersist() {
        if (createdTime == null) createdTime = LocalDateTime.now();
        if (sortOrder == null) sortOrder = 0;
        if (type == null) type = BoardType.FREE;
        if (visibility == null) visibility = Visibility.PUBLIC;
    }

    @PreUpdate
    void preUpdate() { updatedTime = LocalDateTime.now(); }

    public enum BoardType { FREE, ITEM, PROMO, OOTD, SELECT, MARKET }
    public enum Visibility { PUBLIC, PRIVATE, HIDDEN, ARCHIVED }
}
