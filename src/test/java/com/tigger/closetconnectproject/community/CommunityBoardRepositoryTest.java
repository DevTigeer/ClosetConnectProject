package com.tigger.closetconnectproject.community;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigger.closetconnectproject.Common.Exception.GlobalExceptionHandler;
import com.tigger.closetconnectproject.Community.Controller.CommunityBoardController;
import com.tigger.closetconnectproject.Community.Entity.CommunityBoard;
import com.tigger.closetconnectproject.Community.Entity.CommunityBoard.Visibility;
import com.tigger.closetconnectproject.Community.Service.CommunityBoardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CommunityBoardController.class)
@Import(GlobalExceptionHandler.class)
class CommunityBoardControllerTest {

    private static final String API = "/community/boards";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean
    CommunityBoardService boardService;
    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext; // auditing 방지

    private CommunityBoard createBoard(Long id, String slug, Visibility v) {
        return CommunityBoard.builder()
                .id(id)
                .name(slug.toUpperCase())
                .slug(slug)
                .type(CommunityBoard.BoardType.FREE)
                .visibility(v)
                .isSystem(true)
                .sortOrder(10)
                .createdTime(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("공개 보드 전체 목록 조회 성공")
    void getBoards_success() throws Exception {
        given(boardService.listPublic()).willReturn(List.of(createBoard(1L, "free", Visibility.PUBLIC)));

        mockMvc.perform(get(API))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("free"))
                .andExpect(jsonPath("$[0].visibility").value("PUBLIC"));
    }

    @Test
    @DisplayName("특정 슬러그로 보드 조회 성공")
    void getBoardBySlug_success() throws Exception {
        given(boardService.getBySlug(anyString())).willReturn(createBoard(1L, "free", Visibility.PUBLIC));

        mockMvc.perform(get(API + "/free"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("free"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));
    }

    @Test
    @DisplayName("비공개 보드 조회 시 400 에러 발생")
    void getBoardBySlug_hidden() throws Exception {
        given(boardService.getBySlug(anyString())).willReturn(createBoard(1L, "hidden", Visibility.HIDDEN));

        mockMvc.perform(get(API + "/hidden"))
                .andExpect(status().is4xxClientError());
    }
}
