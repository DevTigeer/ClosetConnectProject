package com.tigger.closetconnectproject.Post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigger.closetconnectproject.Post.Controller.AdminPostController;
import com.tigger.closetconnectproject.Post.Dto.PostDtos;
import com.tigger.closetconnectproject.Post.Entity.PostStatus;
import com.tigger.closetconnectproject.Post.Service.PostAdminService;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = AdminPostController.class)
class AdminPostControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    // (선택) WebMvcTest 환경에서 JPA/Auditing로 인한 컨텍스트 경고/충돌 방지
    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockBean AuditorAware<Long> auditorAware;

    @MockBean PostAdminService adminService;

    private Page<PostDtos.PostRes> PAGE;

    @BeforeEach
    void setUp() {
        var p = PostDtos.PostRes.builder()
                .id(99L).title("관리자 글").content("x")
                .authorName("admin").liked(false)
                .viewCount(0L).likeCount(0L)
                .attachments(List.of()).createdAt(LocalDateTime.now()).build();
        PAGE = new PageImpl<>(List.of(p), PageRequest.of(0,20), 1);

        // auditing 목값(없어도 되지만 습관적으로 맞춰둠)
        given(auditorAware.getCurrentAuditor()).willReturn(Optional.of(1L));
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(adminService);
        reset(adminService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 관리자_목록_성공_200() throws Exception {
        // Given
        // 시그니처 가정: (boardId, statuses, keyword, page, size, sort)
        given(adminService.listForAdmin(eq(1L), anyList(), any(), eq(0), eq(20), any()))
                .willReturn(PAGE);

        // When & Then
        mvc.perform(get("/api/v1/admin/boards/{boardId}/posts", 1L)
                        .param("page","0").param("size","20"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)));

        // verify 소진 (matcher 혼용 없이 전부 matcher 사용)
        verify(adminService).listForAdmin(eq(1L), anyList(), any(), eq(0), eq(20), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 상태변경_성공_200() throws Exception {
        // Given
        var req = Map.of("status", "BLINDED");

        // When & Then
        mvc.perform(patch("/api/v1/admin/posts/{postId}/status", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        // verify: updateStatus(postId, PostStatus)
        verify(adminService).updateStatus(eq(99L), eq(PostStatus.BLINDED));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 핀고정_성공_200() throws Exception {
        // Given
        var req = Map.of("pinned", true);

        // When & Then
        mvc.perform(patch("/api/v1/admin/posts/{postId}/pin", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        // verify: pin(postId, pinned)
        verify(adminService).pin(eq(99L), eq(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 이동_성공_200() throws Exception {
        // Given: 타입 혼동 방지를 위해 Long로 맞춤
        var req = Map.of("toBoardId", 2L);

        // When & Then
        mvc.perform(patch("/api/v1/admin/posts/{postId}/move", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        // verify: move(postId, toBoardId)
        verify(adminService).move(eq(99L), eq(2L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 하드삭제_성공_204or200() throws Exception {
        // When & Then
        mvc.perform(delete("/api/v1/admin/posts/{postId}/hard", 99L))
                .andExpect(status().is2xxSuccessful());

        // verify: hardDelete(postId)
        verify(adminService).hardDelete(eq(99L));
    }
}
