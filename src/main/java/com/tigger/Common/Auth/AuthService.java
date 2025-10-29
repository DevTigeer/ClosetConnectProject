package com.tigger.Common.Auth;
import com.tigger.Common.Jwt.JwtTokenProvider;
import com.tigger.User.Dto.LoginRequest;
import com.tigger.User.Dto.SignUpRequest;
import com.tigger.User.Dto.TokenResponse;
import com.tigger.User.Dto.UserSummary;
import com.tigger.User.Entity.UserRole;
import com.tigger.User.Entity.UserStatus;
import com.tigger.User.Entity.Users;
import com.tigger.User.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;

    @Transactional
    public UserSummary signUp(SignUpRequest req) {
        if (usersRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        Users user = Users.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .nickname(req.nickname())
                .name(req.name())
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();
        Users saved = usersRepository.save(user);
        return new UserSummary(saved.getUserId(), saved.getEmail(), saved.getNickname(), saved.getRole().name());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        Users user = usersRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        String token = jwt.createToken(
                user.getEmail(),
                Map.of("uid", user.getUserId(), "role", user.getRole().name())
        );
        UserSummary summary = new UserSummary(user.getUserId(), user.getEmail(), user.getNickname(), user.getRole().name());
        return new TokenResponse(token, summary);
    }

    @Transactional(readOnly = true)
    public UserSummary me(String email) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        return new UserSummary(user.getUserId(), user.getEmail(), user.getNickname(), user.getRole().name());
    }
}

