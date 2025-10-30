package com.tigger.closetconnectproject.User;

import com.tigger.closetconnectproject.Common.Auth.AuthService;
import com.tigger.closetconnectproject.Common.Security.AppUserDetails;
import com.tigger.closetconnectproject.User.Controller.UsersController;
import com.tigger.closetconnectproject.User.Dto.UserSummary;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Entity.UserRole;
import com.tigger.closetconnectproject.User.Entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UsersController.class)
class UsersControllerTest {

    @Autowired
    MockMvc mvc;

    // UsersController가 실제로 주입받는 서비스
    @MockBean
    AuthService authService;

    @Test
    @DisplayName("인증된 사용자는 /users/me 로 내 정보 조회가 된다")
    void me_ok() throws Exception {
        // 1. 도메인 유저 만들어주기 (네 패키지에 맞게)
        Users domainUser = Users.builder()
                .userId(1L)
                .email("u@cc.com")
                .password("ENC")
                .nickname("beom")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();

        // 2. 우리가 만든 시큐리티용 UserDetails
        AppUserDetails principal = new AppUserDetails(domainUser);

        // 3. 시큐리티 Authentication 으로 감싸기
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        // 4. 서비스 모킹 - controller가 부르는 메서드와 인자를 정확하게 맞춰야 함
        given(authService.me("u@cc.com"))
                .willReturn(new UserSummary(1L, "u@cc.com", "beom", "ROLE_USER"));

        // 5. 요청 보낼 때 principal 주입
        mvc.perform(get("/users/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("u@cc.com"))
                .andExpect(jsonPath("$.nickname").value("beom"));
    }

    @Test
    @DisplayName("비로그인 사용자는 /users/me 요청 시 401이 난다")
    void me_unauthorized() throws Exception {
        mvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
