package com.tigger.closetconnectproject.Auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigger.closetconnectproject.User.Dto.LoginRequest;
import com.tigger.closetconnectproject.User.Dto.SignUpRequest;
import com.tigger.closetconnectproject.User.Entity.UserRole;
import com.tigger.closetconnectproject.User.Entity.UserStatus;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        usersRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 성공 - /auth/signup")
    void signUp_success() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("test@cc.com", "12345678", "beom","");

        // when
        mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@cc.com"))
                .andExpect(jsonPath("$.nickname").value("beom"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("로그인 성공 - /auth/login")
    void login_success() throws Exception {
        // given - 사전 회원가입
        String encodedPw = passwordEncoder.encode("12345678");
        usersRepository.save(
                com.tigger.closetconnectproject.User.Entity.Users.builder()
                        .email("login@cc.com")
                        .password(encodedPw)
                        .nickname("tester")
                        .role(UserRole.ROLE_USER)
                        .status(UserStatus.NORMAL)
                        .build()
        );

        LoginRequest request = new LoginRequest("login@cc.com", "12345678");

        // when
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.email").value("login@cc.com"))
                .andExpect(jsonPath("$.user.nickname").value("tester"))
                .andExpect(jsonPath("$.user.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_fail_wrongPassword() throws Exception {
        // given
        String encodedPw = passwordEncoder.encode("12345678");
        usersRepository.save(
                com.tigger.closetconnectproject.User.Entity.Users.builder()
                        .email("fail@cc.com")
                        .password(encodedPw)
                        .nickname("wrong")

                        .role(UserRole.ROLE_USER)
                        .status(UserStatus.NORMAL)
                        .build()
        );

        LoginRequest request = new LoginRequest("fail@cc.com", "wrongpassword");

        // when
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then
                .andExpect(status().is4xxClientError());
    }
}

