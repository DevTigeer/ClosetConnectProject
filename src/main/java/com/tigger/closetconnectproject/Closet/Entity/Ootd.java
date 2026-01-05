package com.tigger.closetconnectproject.Closet.Entity;

import com.tigger.closetconnectproject.Common.Entity.BaseTimeEntity;
import com.tigger.closetconnectproject.User.Entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ootd")
public class Ootd extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(length = 100)
    private String description;

    @Builder
    public Ootd(Users user, String imageUrl, String description) {
        this.user = user;
        this.imageUrl = imageUrl;
        this.description = description;
    }
}
