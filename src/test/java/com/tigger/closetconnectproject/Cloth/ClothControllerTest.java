package com.tigger.closetconnectproject.Cloth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigger.closetconnectproject.Closet.Controller.ClothController;
import com.tigger.closetconnectproject.Closet.Dto.ClothCreateRequest;
import com.tigger.closetconnectproject.Closet.Dto.ClothResponse;
import com.tigger.closetconnectproject.Closet.Entity.Category;
import com.tigger.closetconnectproject.Closet.Service.ClothService;
import com.tigger.closetconnectproject.Common.Exception.GlobalExceptionHandler;
import com.tigger.closetconnectproject.Common.Security.AppUserDetails;
import com.tigger.closetconnectproject.User.Entity.UserRole;
import com.tigger.closetconnectproject.User.Entity.UserStatus;
import com.tigger.closetconnectproject.User.Entity.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.mockito.ArgumentMatchers.*; // any, anyLong, eq 등

@WebMvcTest(controllers = ClothController.class)
@Import(GlobalExceptionHandler.class)
class ClothControllerTest {

    private static final String API = "/api/v1/cloth";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ClothService clothService;

    // JPA Auditing 에러 방지용
    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private AppUserDetails testUserDetails() {
        Users domainUser = Users.builder()
                .userId(10L)
                .email("u@cc.com")
                .password("ENC")
                .nickname("beom")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();
        return new AppUserDetails(domainUser);
    }

    @Test
    @DisplayName("인증된 사용자는 옷을 등록할 수 있다")
    void create_success() throws Exception {
        ClothCreateRequest req = new ClothCreateRequest(
                "블랙 티셔츠", Category.TOP, "BLACK", "UNIQLO", "http://img/1"
        );

        ClothResponse res = new ClothResponse(
                1L, "블랙 티셔츠", Category.TOP, "BLACK", "UNIQLO", "http://img/1"
        );

        given(clothService.create(anyLong(), any(ClothCreateRequest.class)))
                .willReturn(res);

        mockMvc.perform(
                        post(API)
                                .with(user(testUserDetails()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("블랙 티셔츠"))
                // ✅ Enum은 문자열로 직렬화되므로 "TOP"로 검증
                .andExpect(jsonPath("$.category").value("TOP"))
                .andExpect(jsonPath("$.brand").value("UNIQLO"));
    }

    @Test
    @DisplayName("비로그인 사용자는 옷 등록 시 401이 발생한다")
    void create_unauthorized() throws Exception {
        ClothCreateRequest req = new ClothCreateRequest(
                "블랙 티셔츠", Category.TOP, "BLACK", "UNIQLO", "http://img/1"
        );

        mockMvc.perform(
                        post(API)
                                .with(csrf()) // CSRF 통과 → 인증 없어서 401
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증된 사용자는 특정 옷을 조회할 수 있다")
    void getCloth_success() throws Exception {
        ClothResponse res = new ClothResponse(
                1L, "블랙 티셔츠", Category.TOP, "BLACK", "UNIQLO", "http://img/1"
        );

        // ✅ principal(userId=10L) + id=1L 로 스텁
        given(clothService.getOne(eq(10L), eq(1L)))
                .willReturn(res);

        AppUserDetails principal = testUserDetails();

        mockMvc.perform(
                        get(API + "/{id}", 1L)
                                .with(user(principal))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("블랙 티셔츠"))
                // Enum 직렬화는 문자열 → "TOP"
                .andExpect(jsonPath("$.category").value("TOP"));
    }

    @Test
    @DisplayName("존재하지 않는 옷을 조회하면 404가 발생한다")
    void getCloth_notFound() throws Exception {
        // ✅ principal(userId=10L) + id=999L 로 스텁
        given(clothService.getOne(eq(10L), eq(999L)))
                .willThrow(new IllegalArgumentException("옷을 찾을 수 없습니다."));

        AppUserDetails principal = testUserDetails();

        mockMvc.perform(
                        get(API + "/{id}", 999L)
                                .with(user(principal))
                )
                .andExpect(status().isNotFound());
    }

}
