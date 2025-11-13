package com.tigger.closetconnectproject.Community.Controller;

import com.tigger.closetconnectproject.Community.Dto.BoardDtos.BoardRes;
import com.tigger.closetconnectproject.Community.Entity.CommunityBoard;
import com.tigger.closetconnectproject.Community.Service.CommunityBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/api/v1/community/boards")
@RequiredArgsConstructor
public class CommunityBoardController {

    private final CommunityBoardService service;

    // 공개 보드 목록
    @GetMapping
    public List<BoardRes> listPublicBoards() {
        return service.listPublic().stream().map(BoardRes::of).collect(toList());
    }

    // 슬러그로 보드 조회(비공개/숨김 보드는 404로 처리하는 게 안전)
    @GetMapping("/{slug}")
    public BoardRes getBoard(@PathVariable String slug) {
        CommunityBoard b = service.getBySlug(slug);
        if (b.getVisibility() != CommunityBoard.Visibility.PUBLIC) {
            throw new IllegalArgumentException("board not public");
        }
        return BoardRes.of(b);
    }
}

