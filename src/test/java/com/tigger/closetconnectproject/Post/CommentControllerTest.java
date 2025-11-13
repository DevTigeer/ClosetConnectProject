package com.tigger.closetconnectproject.Post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigger.closetconnectproject.Security.AppUserDetails;
import com.tigger.closetconnectproject.Post.Controller.CommentController;
import com.tigger.closetconnectproject.Post.Dto.CommentDtos;
import com.tigger.closetconnectproject.Post.Service.CommentService;
import com.tigger.closetconnectproject.User.Entity.UserRole;
import com.tigger.closetconnectproject.User.Entity.UserStatus;
import com.tigger.closetconnectproject.User.Entity.Users;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = CommentController.class)
class CommentControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    // JPA/Auditing 충돌 방지용 mock
    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockBean AuditorAware<Long> auditorAware;

    @MockBean CommentService commentService;

    // 공통 로그인(principal = userId 99, ROLE_USER)
    private Users mockUser;
    private AppUserDetails principal;
    private Authentication auth;

    private CommentDtos.CommentRes C1, C2;
    private Page<CommentDtos.CommentRes> PAGE;

    @BeforeEach
    void setUp() {
        mockUser = Users.builder()
                .userId(99L)
                .email("test@test.com")
                .password("encoded")
                .nickname("테스터")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();
        principal = new AppUserDetails(mockUser);
        auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        given(auditorAware.getCurrentAuditor()).willReturn(Optional.of(99L));

        C1 = CommentDtos.CommentRes.builder()
                .id(11L).postId(101L).parentId(null)
                .content("첫 댓글").authorName("kim")
                .createdAt(LocalDateTime.now()).build();
        C2 = CommentDtos.CommentRes.builder()
                .id(12L).postId(101L).parentId(11L)
                .content("대댓글").authorName("lee")
                .createdAt(LocalDateTime.now()).build();
        PAGE = new PageImpl<>(List.of(C1, C2), PageRequest.of(0, 50), 2);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(commentService);
        reset(commentService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void 목록_성공_200() throws Exception {
        // Given
        given(commentService.list(101L, 0, 50)).willReturn(PAGE);

        // When & Then
        var ra = mvc.perform(get("/api/v1/posts/{postId}/comments", 101L)
                        .param("page", "0").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].content").value("첫 댓글"))
                .andReturn();

        // (옵션) 눈에 보이는 expect/actual
        var body = ra.getResponse().getContentAsString();
        var firstContent = om.readTree(body).get("content").get(0).get("content").asText();
        assertThat(firstContent)
                .withFailMessage("expect: %s, actual: %s", "첫 댓글", firstContent)
                .isEqualTo("첫 댓글");

        // 서비스 호출 소진
        verify(commentService).list(101L, 0, 50);
    }

    @Test
    void 작성_성공_200() throws Exception {
        // Given
        var req = new CommentDtos.CreateReq();
        req.setContent("새 댓글");
        var created = CommentDtos.CommentRes.builder()
                .id(13L).postId(101L).parentId(null)
                .content("새 댓글").authorName("me")
                .createdAt(LocalDateTime.now()).build();

        // principal 주입으로 userId=99L → 스텁/검증도 99L로 맞춤
        given(commentService.create(eq(101L), eq(99L), any(CommentDtos.CreateReq.class)))
                .willReturn(created);

        // When & Then
        mvc.perform(post("/api/v1/posts/{postId}/comments", 101L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(13))
                .andExpect(jsonPath("$.content").value("새 댓글"));

        // 서비스 호출 소진
        verify(commentService).create(eq(101L), eq(99L), any(CommentDtos.CreateReq.class));
    }

    @Test
    void 수정_성공_200() throws Exception {
        // Given
        var req = new CommentDtos.UpdateReq();
        req.setContent("수정");
        var updated = CommentDtos.CommentRes.builder()
                .id(11L).postId(101L).parentId(null)
                .content("수정").authorName("kim")
                .createdAt(LocalDateTime.now()).build();

        // isAdmin=false (ROLE_USER)로 엄격 검증
        given(commentService.update(eq(11L), eq(99L), any(CommentDtos.UpdateReq.class), eq(false)))
                .willReturn(updated);

        // When & Then
        mvc.perform(patch("/api/v1/posts/{postId}/comments/{commentId}", 101L, 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").value("수정"));

        // 서비스 호출 소진
        verify(commentService).update(eq(11L), eq(99L), any(CommentDtos.UpdateReq.class), eq(false));
    }

    @Test
    void 삭제_성공_204or200() throws Exception {
        // When & Then
        mvc.perform(delete("/api/v1/posts/{postId}/comments/{commentId}", 101L, 11L))
                .andExpect(status().is2xxSuccessful());

        // ROLE_USER → isAdmin=false
        verify(commentService).delete(eq(11L), eq(99L), eq(false));
    }
}
