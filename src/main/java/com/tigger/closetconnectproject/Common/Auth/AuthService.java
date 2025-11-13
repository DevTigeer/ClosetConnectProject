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

/**
 * 인증/회원가입 관련 비즈니스 로직을 처리하는 Service
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;

    /**
     * 설명: 신규 사용자 회원가입 처리
     * - 이메일 중복 체크
     * - 닉네임 중복 체크
     * - 비밀번호 BCrypt 암호화
     * - 기본 권한(ROLE_USER), 상태(NORMAL) 설정
     * @param req 회원가입 요청 정보 (email, password, nickname, name)
     * @return 생성된 사용자 요약 정보
     */
    @Transactional
    public UserSummary signUp(SignUpRequest req) {
        // 이메일 중복 체크
        if (usersRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 닉네임 중복 체크 추가
        if (usersRepository.existsByNickname(req.nickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        Users user = Users.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password())) // BCrypt 암호화
                .nickname(req.nickname())
                .name(req.name())
                .role(UserRole.ROLE_USER) // 기본 권한: 일반 사용자
                .status(UserStatus.NORMAL) // 기본 상태: 정상
                .build();
        Users saved = usersRepository.save(user);
        return new UserSummary(saved.getUserId(), saved.getEmail(), saved.getNickname(), saved.getRole().name());
    }

    /**
     * 설명: 로그인 처리 및 JWT 토큰 발급
     * - 이메일로 사용자 조회
     * - 비밀번호 검증 (BCrypt)
     * - JWT 토큰 생성 (subject=email, claims=uid+role)
     * @param req 로그인 요청 정보 (email, password)
     * @return JWT 토큰 + 사용자 요약 정보
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        // 사용자 조회
        Users user = usersRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BadCredentialsException("아이디나 비밀번호가 올바르지 않습니다.");
        }

        // JWT 토큰 생성 (subject: email, claims: userId + role)
        String token = jwt.createToken(
                user.getEmail(),
                Map.of("uid", user.getUserId(), "role", user.getRole().name())
        );
        UserSummary summary = new UserSummary(user.getUserId(), user.getEmail(), user.getNickname(), user.getRole().name());
        return new TokenResponse(token, summary);
    }

    /**
     * 설명: 현재 로그인한 사용자 정보 조회
     * @param email 사용자 이메일 (JWT subject에서 추출)
     * @return 사용자 요약 정보
     */
    @Transactional(readOnly = true)
    public UserSummary me(String email) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        return new UserSummary(user.getUserId(), user.getEmail(), user.getNickname(), user.getRole().name());
    }
}

