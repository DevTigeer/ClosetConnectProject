package com.tigger.closetconnectproject.Auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigger.closetconnectproject.User.Dto.LoginRequest;
import com.tigger.closetconnectproject.User.Dto.SignUpRequest;
import com.tigger.closetconnectproject.User.Entity.UserRole;
import com.tigger.closetconnectproject.User.Entity.UserStatus;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 성공 - /api/v1/auth/signup")
    void signUp_success() throws Exception {
        // given
        String uniqueEmail = "test" + System.currentTimeMillis() + "@cc.com";
        String uniqueNickname = "user" + System.currentTimeMillis();
        SignUpRequest request = new SignUpRequest(uniqueEmail, "12345678", uniqueNickname, "테스터");

        // when
        mvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(uniqueEmail))
                .andExpect(jsonPath("$.nickname").value(uniqueNickname))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("로그인 성공 - /api/v1/auth/login")
    void login_success() throws Exception {
        // given - 사전 회원가입
        String uniqueEmail = "login" + System.currentTimeMillis() + "@cc.com";
        String uniqueNickname = "tester" + System.currentTimeMillis();
        String encodedPw = passwordEncoder.encode("12345678");
        usersRepository.save(
                com.tigger.closetconnectproject.User.Entity.Users.builder()
                        .email(uniqueEmail)
                        .password(encodedPw)
                        .nickname(uniqueNickname)
                        .name("로그인테스터")
                        .role(UserRole.ROLE_USER)
                        .status(UserStatus.NORMAL)
                        .build()
        );

        LoginRequest request = new LoginRequest(uniqueEmail, "12345678");

        // when
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.email").value(uniqueEmail))
                .andExpect(jsonPath("$.user.nickname").value(uniqueNickname))
                .andExpect(jsonPath("$.user.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_fail_wrongPassword() throws Exception {
        // given
        String uniqueEmail = "fail" + System.currentTimeMillis() + "@cc.com";
        String uniqueNickname = "wrong" + System.currentTimeMillis();
        String encodedPw = passwordEncoder.encode("12345678");
        usersRepository.save(
                com.tigger.closetconnectproject.User.Entity.Users.builder()
                        .email(uniqueEmail)
                        .password(encodedPw)
                        .nickname(uniqueNickname)
                        .name("실패테스터")
                        .role(UserRole.ROLE_USER)
                        .status(UserStatus.NORMAL)
                        .build()
        );

        LoginRequest request = new LoginRequest(uniqueEmail, "wrongpassword");

        // when
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then
                .andExpect(status().is4xxClientError());
    }
}

