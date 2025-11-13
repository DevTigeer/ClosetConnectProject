package com.tigger.closetconnectproject.User.Entity;

import com.tigger.closetconnectproject.Common.Entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Users",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_users_email", columnNames = "email"),
                @UniqueConstraint(name = "UK_users_nickname", columnNames = "nickname")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Users extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;   // BCrypt

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 20)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status; // NORMAL, SUSPENDED, DELETED

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;     // ROLE_USER, ROLE_ADMIN
}

