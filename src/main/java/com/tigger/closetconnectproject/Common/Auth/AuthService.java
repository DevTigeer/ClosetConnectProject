package com.tigger.closetconnectproject.Common.Auth;
import com.tigger.closetconnectproject.Common.Jwt.JwtTokenProvider;
import com.tigger.closetconnectproject.User.Dto.LoginRequest;
import com.tigger.closetconnectproject.User.Dto.SignUpRequest;
import com.tigger.closetconnectproject.User.Dto.TokenResponse;
import com.tigger.closetconnectproject.User.Dto.UserSummary;
import com.tigger.closetconnectproject.User.Entity.UserRole;
import com.tigger.closetconnectproject.User.Entity.UserStatus;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import org.springframework.web.server.ResponseStatusException;

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
            throw new BadCredentialsException("아이디나 비밀번호가 올바르지 않습니다.");
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

