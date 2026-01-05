package com.tigger.closetconnectproject.Community;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigger.closetconnectproject.Community.Controller.AdminBoardController;
import com.tigger.closetconnectproject.Community.Dto.BoardDtos;
import com.tigger.closetconnectproject.Community.Entity.CommunityBoard;
import com.tigger.closetconnectproject.Community.Entity.CommunityBoard.BoardType;
import com.tigger.closetconnectproject.Community.Entity.CommunityBoard.Visibility;
import com.tigger.closetconnectproject.Community.Service.CommunityBoardService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
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
@WebMvcTest(controllers = AdminBoardController.class)
class AdminBoardControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockBean AuditorAware<Long> auditorAware;

    @MockBean CommunityBoardService service;

    private CommunityBoard existing, created, updated, visChanged;

    @BeforeEach
    void setUp() {
        given(auditorAware.getCurrentAuditor()).willReturn(Optional.of(1L));

        existing = CommunityBoard.builder()
                .id(1L).name("공지사항").slug("notice")
                .type(BoardType.FREE).visibility(Visibility.PUBLIC)
                .isSystem(true).sortOrder(0)
                .build();

        created = CommunityBoard.builder()
                .id(3L).name("신규 게시판").slug("new-board")
                .type(BoardType.OOTD).visibility(Visibility.PUBLIC)
                .isSystem(false).sortOrder(5)
                .build();

        updated = CommunityBoard.builder()
                .id(1L).name("수정됨").slug("notice")
                .type(BoardType.FREE).visibility(Visibility.PUBLIC)
                .isSystem(true).sortOrder(3)
                .build();

        visChanged = CommunityBoard.builder()
                .id(1L).name("공지사항").slug("notice")
                .type(BoardType.FREE).visibility(Visibility.HIDDEN)
                .isSystem(true).sortOrder(0)
                .build();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(service);
        reset(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 관리자_목록_성공_200() throws Exception {
        // Given
        given(service.listAllForAdmin()).willReturn(List.of(existing));

        // When & Then
        mvc.perform(get("/api/v1/admin/community/boards"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].slug").value("notice"));

        // verify 소진
        verify(service).listAllForAdmin();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 관리자_생성_성공_201or200() throws Exception {
        // Given: 컨트롤러가 엔티티를 만들어 service.create(b) 호출 → 우리는 반환값만 맞추면 됨
        var req = new BoardDtos.CreateBoardReq(
                "신규 게시판", "new-board", "OOTD", "PUBLIC", false, 5
        );
        given(service.create(any(CommunityBoard.class))).willReturn(created);

        // When & Then
        mvc.perform(post("/api/v1/admin/community/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("신규 게시판"))
                .andExpect(jsonPath("$.type").value("OOTD"));

        // verify 소진 (필드 검증이 필요하면 argThat으로 확장 가능)
        verify(service).create(any(CommunityBoard.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 관리자_수정_성공_200() throws Exception {
        // Given
        var req = new BoardDtos.UpdateBoardReq("수정됨", 3);
        given(service.update(eq(1L), eq("수정됨"), eq(3))).willReturn(updated);

        // When & Then
        mvc.perform(patch("/api/v1/admin/community/boards/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("수정됨"))
                .andExpect(jsonPath("$.sortOrder").value(3));

        // verify 소진
        verify(service).update(eq(1L), eq("수정됨"), eq(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 관리자_공개범위변경_성공_200() throws Exception {
        // Given
        var req = new BoardDtos.ChangeVisibilityReq("HIDDEN");
        given(service.changeVisibility(eq(1L), eq(Visibility.HIDDEN))).willReturn(visChanged);

        // When & Then
        mvc.perform(patch("/api/v1/admin/community/boards/{id}/visibility", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.visibility").value("HIDDEN"));

        // verify 소진
        verify(service).changeVisibility(eq(1L), eq(Visibility.HIDDEN));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 관리자_삭제_성공_204or200() throws Exception {
        // When & Then
        mvc.perform(delete("/api/v1/admin/community/boards/{id}", 1L))
                .andExpect(status().is2xxSuccessful());

        // verify 소진
        verify(service).softDelete(eq(1L));
    }
}
