package com.tigger.closetconnectproject.Post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigger.closetconnectproject.Security.AppUserDetails;
import com.tigger.closetconnectproject.Post.Controller.PostController;
import com.tigger.closetconnectproject.Post.Dto.PostDtos;
import com.tigger.closetconnectproject.Post.Entity.Visibility;
import com.tigger.closetconnectproject.Post.Service.PostService;
import com.tigger.closetconnectproject.User.Entity.UserRole;
import com.tigger.closetconnectproject.User.Entity.UserStatus;
import com.tigger.closetconnectproject.User.Entity.Users;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 보안 필터 off → URL/JSON만 테스트 (메서드 보안은 @WithMockUser로 처리)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = PostController.class)
class PostControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext; // 메타모델 빈 목업

    @MockBean
    AuditorAware<Long> auditorAware; // Auditing에서 참조 (타입은 너희 프로젝트 설정에 맞게)
    @MockBean PostService postService;
    @MockBean com.tigger.closetconnectproject.Post.Service.PostLikeService postLikeService;

    // ===== 기대 응답(고정) =====
    private PostDtos.PostRes POST1;
    private PostDtos.PostRes POST2;
    private Page<PostDtos.PostRes> PAGE;

    @BeforeEach
    void setUp() {
        POST1 = PostDtos.PostRes.builder()
                .id(101L)
                .title("첫 글").content("본문1")
                .authorName("kim").liked(false)
                .viewCount(10L).likeCount(2L)
                .attachments(List.of())
                .createdAt(LocalDateTime.now())
                .build();
        POST2 = PostDtos.PostRes.builder()
                .id(102L)
                .title("둘째 글").content("본문2")
                .authorName("lee").liked(true)
                .viewCount(5L).likeCount(7L)
                .attachments(List.of())
                .createdAt(LocalDateTime.now())
                .build();
        PAGE = new PageImpl<>(List.of(POST1, POST2), PageRequest.of(0, 20), 2);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(postService);
        Mockito.reset(postService);
    }

    @Test
    void 목록조회_성공_200() throws Exception {
        // Given
        given(postService.list(eq(1L), anyInt(), anyInt(), anyString(), any(), any()))
                .willReturn(PAGE);

        // When & Then
        mvc.perform(get("/api/v1/boards/{boardId}/posts", 1L)
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "LATEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(101))
                .andExpect(jsonPath("$.content[1].title").value("둘째 글"));

        // 👉 이번 요청에서 컨트롤러가 서비스에 정확히 이렇게 넘겼는지 검증
        //   - page=0, size=20, sort=LATEST
        //   - q=null, viewerId=null (비로그인/쿼리 미지정 가정)
        verify(postService).list(eq(1L), eq(0), eq(20), eq("LATEST"), isNull(), isNull());
    }

    @Test
    @WithMockUser
        // 작성은 인증 필요
    void 작성_성공_201or200() throws Exception {
        // Given
        var mockUser = Users.builder()
                .userId(99L)
                .email("test@test.com")
                .nickname("테스트유저")
                .password("encoded")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();
        var principal = new AppUserDetails(mockUser);

        // principal을 SecurityContext에 넣기
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        var req = new PostDtos.CreateReq();
        req.setTitle("새 글"); req.setContent("본문"); req.setVisibility(Visibility.PUBLIC);

        var created = PostDtos.PostRes.builder()
                .id(201L).title("새 글").content("본문")
                .authorName("kim").liked(false).viewCount(0L).likeCount(0L)
                .attachments(List.of()).createdAt(LocalDateTime.now()).build();

        given(postService.create(eq(1L), anyLong(), any(PostDtos.CreateReq.class)))
                .willReturn(created);

        // When & Then
        mvc.perform(post("/api/v1/boards/{boardId}/posts", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.id").value(201))
                .andExpect(jsonPath("$.title").value("새 글"));
                verify(postService).create(eq(1L), eq(99L), any(PostDtos.CreateReq.class));

    }
}

