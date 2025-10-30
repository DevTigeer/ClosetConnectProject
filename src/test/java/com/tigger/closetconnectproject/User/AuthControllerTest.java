package com.tigger.closetconnectproject.User;

import com.tigger.closetconnectproject.Common.Auth.AuthService;
import com.tigger.closetconnectproject.Common.Jwt.JwtTokenProvider;
import com.tigger.closetconnectproject.User.Dto.LoginRequest;
import com.tigger.closetconnectproject.User.Dto.SignUpRequest;
import com.tigger.closetconnectproject.User.Dto.TokenResponse;
import com.tigger.closetconnectproject.User.Dto.UserSummary;
import com.tigger.closetconnectproject.User.Entity.UserRole;
import com.tigger.closetconnectproject.User.Entity.UserStatus;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UsersRepository usersRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock
    JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    AuthService authService;

    @Test
    @DisplayName("회원가입 성공")
    void signUp_success() {
        SignUpRequest req = new SignUpRequest("u@cc.com", "pw", "beom", "범");
        given(usersRepository.existsByEmail("u@cc.com")).willReturn(false);
        given(passwordEncoder.encode("pw")).willReturn("ENC");
        Users saved = Users.builder()
                .userId(1L)
                .email("u@cc.com")
                .password("ENC")
                .nickname("beom")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();
        given(usersRepository.save(any(Users.class))).willReturn(saved);

        UserSummary summary = authService.signUp(req);

        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.email()).isEqualTo("u@cc.com");
        verify(usersRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signUp_dup() {
        SignUpRequest req = new SignUpRequest("dup@cc.com", "pw", "nick", null);
        given(usersRepository.existsByEmail("dup@cc.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signUp(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 가입된 이메일");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        LoginRequest req = new LoginRequest("u@cc.com", "pw");
        Users user = Users.builder()
                .userId(10L)
                .email("u@cc.com")
                .password("ENC")
                .nickname("beom")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();
        given(usersRepository.findByEmail("u@cc.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pw", "ENC")).willReturn(true);
        given(jwtTokenProvider.createToken(eq("u@cc.com"), anyMap())).willReturn("TOKEN");

        TokenResponse res = authService.login(req);

        assertThat(res.accessToken()).isEqualTo("TOKEN");
        assertThat(res.user().email()).isEqualTo("u@cc.com");
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 없음")
    void login_noEmail() {
        LoginRequest req = new LoginRequest("no@cc.com", "pw");
        given(usersRepository.findByEmail("no@cc.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 계정");
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 틀림")
    void login_wrongPw() {
        LoginRequest req = new LoginRequest("u@cc.com", "pw");
        Users user = Users.builder()
                .userId(10L)
                .email("u@cc.com")
                .password("ENC")
                .nickname("beom")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();
        given(usersRepository.findByEmail("u@cc.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pw", "ENC")).willReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("아이디나 비밀번호가 올바르지 않습니다.");
    }
}
