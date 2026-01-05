package com.tigger.closetconnectproject.Community.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
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

    private LocalDateTime deletedAt;

    @Builder
    public CommunityBoard(Long id, String name, String slug, BoardType type,
                          Visibility visibility, boolean isSystem, Integer sortOrder) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.type = type;
        this.visibility = visibility;
        this.isSystem = isSystem;
        this.sortOrder = sortOrder;
    }

    @PrePersist
    void prePersist() {
        if (sortOrder == null) sortOrder = 0;
        if (type == null) type = BoardType.FREE;
        if (visibility == null) visibility = Visibility.PUBLIC;
    }

    public enum BoardType { FREE, ITEM, PROMO, OOTD, SELECT, MARKET }
    public enum Visibility { PUBLIC, PRIVATE, HIDDEN, ARCHIVED }
}
