package com.tigger.closetconnectproject.User;

import com.tigger.closetconnectproject.Common.Auth.AuthService;
import com.tigger.closetconnectproject.Common.Exception.GlobalExceptionHandler;
import com.tigger.closetconnectproject.Common.Security.AppUserDetails;
import com.tigger.closetconnectproject.User.Controller.UsersController;
import com.tigger.closetconnectproject.User.Dto.UserSummary;
import com.tigger.closetconnectproject.User.Entity.UserRole;
import com.tigger.closetconnectproject.User.Entity.UserStatus;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UsersController.class)
@Import(GlobalExceptionHandler.class)
class UsersControllerTest {

    private static final String API = "/api/v1";

    @Autowired
    MockMvc mockMvc;

    // 컨트롤러가 주입받는 것들 mock
    @MockBean
    AuthService authService;

    @MockBean
    UserService userService;

    // JPA Auditing 때문에 필요
    @MockBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    // 테스트에서 쓸 가짜 사용자
    private AppUserDetails testUserDetails() {
        Users domainUser = Users.builder()
                .userId(1L)
                .email("u@cc.com")
                .password("ENC")
                .nickname("beom")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();
        return new AppUserDetails(domainUser);
    }

    @Test
    @DisplayName("인증된 사용자는 /api/v1/users/me 로 내 정보 조회가 된다")
    void me_ok() throws Exception {
        // 서비스가 뭐라고 응답할지 정의
        given(authService.me("u@cc.com"))
                .willReturn(new UserSummary(1L, "u@cc.com", "beom", "ROLE_USER"));

        AppUserDetails principal = testUserDetails();

        mockMvc.perform(
                        get(API + "/users/me")
                                // 👇 여기서 실제로 시큐리티를 탄다
                                .with(user(principal))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("u@cc.com"))
                .andExpect(jsonPath("$.nickname").value("beom"));
    }

    @Test
    @DisplayName("비로그인 사용자는 /api/v1/users/me 요청 시 401이 난다")
    void me_unauthorized() throws Exception {
        mockMvc.perform(get(API + "/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증된 사용자는 /api/v1/users/{id} 로 특정 사용자 정보 조회가 된다")
    void getUser_success() throws Exception {
        // given
        UserSummary mockUser = new UserSummary(1L, "test@naver.com", "범", "ROLE_USER");
        given(userService.getUserById(anyLong()))
                .willReturn(mockUser);

        AppUserDetails principal = testUserDetails();

        mockMvc.perform(
                        get(API + "/users/{id}", 1L)
                                .with(user(principal))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))             // ← $.data.userId 말고 이거
                .andExpect(jsonPath("$.email").value("test@naver.com"))
                .andExpect(jsonPath("$.nickname").value("범"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));

    }

    @Test
    @DisplayName("인증된 사용자라도 존재하지 않는 ID면 404가 난다")
    void getUser_notFound() throws Exception {
        given(userService.getUserById(anyLong()))
                .willThrow(new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        AppUserDetails principal = testUserDetails();

        mockMvc.perform(
                        get(API + "/users/{id}", 9999L)
                                .with(user(principal))          // 👈 인증 있음
                )
                // 이제는 401이 아니라 404가 나와야 함
                .andExpect(status().isNotFound());
    }
}
