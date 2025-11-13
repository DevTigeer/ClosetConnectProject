package com.tigger.closetconnectproject.Post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigger.closetconnectproject.Security.AppUserDetails;
import com.tigger.closetconnectproject.Post.Controller.PostReadController;
import com.tigger.closetconnectproject.Post.Dto.PostDtos;
import com.tigger.closetconnectproject.Post.Entity.Visibility;
import com.tigger.closetconnectproject.Post.Service.PostService;
import com.tigger.closetconnectproject.User.Entity.UserRole;
import com.tigger.closetconnectproject.User.Entity.UserStatus;
import com.tigger.closetconnectproject.User.Entity.Users;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = PostReadController.class)
class PostReadControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    // ✅ Auditing/JPA 메타모델 충돌 방지
    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockBean AuditorAware<Long> auditorAware;

    // ✅ 우리가 실제 쓰는 서비스만 Mock
    @MockBean PostService postService;

    // ✅ 공통 로그인 객체
    private Users mockUser;
    private AppUserDetails principal;
    private Authentication auth;

    private PostDtos.PostRes DETAIL;

    @BeforeEach
    void setUp() {
        // 로그인 세팅
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

        // 공통 상세 응답 고정
        DETAIL = PostDtos.PostRes.builder()
                .id(101L).boardId(1L)
                .title("상세").content("본문")
                .authorName("kim").likedByMe(false)
                .viewCount(11).likeCount(3)
                .attachments(List.of())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        // 각 테스트에서 verify(...)로 '소진'된 호출 외엔 없어야 함
        verifyNoMoreInteractions(postService);
        reset(postService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void 조회_성공_200() throws Exception {
        given(postService.read(101L, 99L)).willReturn(DETAIL);

        // When
        var ra = mvc.perform(get("/api/v1/posts/{postId}", 101L))
                .andExpect(status().isOk())
                // ✅ 이제 JSON 본문과 타입을 기대
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.title").value("상세"));

        // Then: 눈으로 비교(옵션)
        var expected = Map.of("id", 101);
        var actualJson = ra.andReturn().getResponse().getContentAsString();
        var actualId = om.readTree(actualJson).get("id").asInt();
        assertThat(actualId)
                .withFailMessage("expect: %s, actual: %s", expected.get("id"), actualId)
                .isEqualTo(expected.get("id"));

        // 서비스 호출 소진
        verify(postService).increaseView(101L);
        verify(postService).read(101L, 99L);
    }

    @Test
    void 수정_성공_200() throws Exception {
        // Given
        var req = new PostDtos.UpdateReq();
        req.setTitle("수정"); req.setContent("수정 본문"); req.setVisibility(Visibility.PUBLIC);

        var updated = PostDtos.PostRes.builder()
                .id(101L).boardId(1L)
                .title("수정").content("수정 본문")
                .authorName("kim").likedByMe(false)
                .viewCount(11).likeCount(3)
                .attachments(List.of())
                .createdAt(LocalDateTime.now()).build();

        given(postService.update(eq(101L), eq(99L), any(PostDtos.UpdateReq.class), anyBoolean()))
                .willReturn(updated);

        // When & Then
        mvc.perform(patch("/api/v1/posts/{postId}", 101L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정"));

        // 서비스 호출 소진
        verify(postService).update(eq(101L), eq(99L), any(PostDtos.UpdateReq.class), anyBoolean());
    }

    @Test
    void 삭제_성공_204or200() throws Exception {
        // When & Then
        mvc.perform(delete("/api/v1/posts/{postId}", 101L))
                .andExpect(status().is2xxSuccessful());

        // 서비스 호출 소진
        // 컨트롤러 시그니처에 맞춰 아래 중 하나로 검증
        // 1) 작성자 본인 삭제라면:
        // verify(postService).delete(eq(101L), eq(99L), anyBoolean());
        // 2) 단순 soft-delete라면:
        verify(postService).delete(eq(101L), eq(99L),eq(false));
    }

    @Test
    void 좋아요_성공_204or200() throws Exception {
        mvc.perform(post("/api/v1/posts/{postId}/like", 101L))
                .andExpect(status().is2xxSuccessful());
        verify(postService).like(101L, 99L);
    }

    @Test
    void 좋아요취소_성공_204or200() throws Exception {
        mvc.perform(delete("/api/v1/posts/{postId}/like", 101L))
                .andExpect(status().is2xxSuccessful());
        verify(postService).unlike(101L, 99L);
    }

    @Test
    void 첨부업로드_성공_200() throws Exception {
        // Given
        var file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1,2,3});
        var att = PostDtos.AttachmentRes.builder()
                .id(1L).url("/uploads/1/2025/11/10/a.jpg")
                .filename("a.jpg").contentType("image/jpeg").size(3L).build();
        given(postService.uploadAttachment(eq(101L), eq(99L), any())).willReturn(att);

        // When & Then
        mvc.perform(multipart("/api/v1/posts/{postId}/attachments", 101L).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("a.jpg"));

        // 서비스 호출 소진
        verify(postService).uploadAttachment(eq(101L), eq(99L), any());
    }
}
