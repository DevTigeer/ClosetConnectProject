package com.tigger.closetconnectproject.Community;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigger.closetconnectproject.Community.Controller.CommunityBoardController;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = CommunityBoardController.class)
class CommunityBoardControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    // 컨텍스트 안정화용 (경고/충돌 방지)
    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockBean AuditorAware<Long> auditorAware;

    @MockBean CommunityBoardService service;

    private CommunityBoard b1, b2;

    @BeforeEach
    void setUp() {
        given(auditorAware.getCurrentAuditor()).willReturn(Optional.of(1L));

        b1 = CommunityBoard.builder()
                .id(1L).name("공지사항").slug("notice")
                .type(BoardType.FREE).visibility(Visibility.PUBLIC)
                .isSystem(true).sortOrder(0)
                .build();

        b2 = CommunityBoard.builder()
                .id(2L).name("잡담").slug("talk")
                .type(BoardType.FREE).visibility(Visibility.PUBLIC)
                .isSystem(false).sortOrder(1)
                .build();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(service);
        reset(service);
    }

    @Test
    void 공개_보드목록_성공_200() throws Exception {
        // Given
        given(service.listPublic()).willReturn(List.of(b1, b2));

        // When & Then
        mvc.perform(get("/api/v1/community/boards"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("공지사항"))
                .andExpect(jsonPath("$[1].slug").value("talk"));

        // verify 소진
        verify(service).listPublic();
    }

    @Test
    void 슬러그_단건조회_성공_200() throws Exception {
        // Given
        given(service.getBySlug(eq("notice"))).willReturn(b1);

        // When & Then
        mvc.perform(get("/api/v1/community/boards/{slug}", "notice"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));

        // verify 소진
        verify(service).getBySlug(eq("notice"));
    }
}

