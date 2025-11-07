package com.tigger.closetconnectproject.Community.Controller;

import com.tigger.closetconnectproject.Community.Dto.BoardDtos;
import com.tigger.closetconnectproject.Community.Dto.BoardDtos.BoardRes;
import com.tigger.closetconnectproject.Community.Entity.CommunityBoard;
import com.tigger.closetconnectproject.Community.Service.CommunityBoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/api/v1/admin/community/boards")
@RequiredArgsConstructor
public class AdminBoardController {

    private final CommunityBoardService service;

    // 관리자 목록(숨김/비공개 포함)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<BoardRes> listAll() {
        return service.listAllForAdmin().stream().map(BoardRes::of).collect(toList());
    }

    // 생성
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public BoardRes create(@Valid @RequestBody BoardDtos.CreateBoardReq req) {
        CommunityBoard b = CommunityBoard.builder()
                .name(req.name())
                .slug(req.slug())
                .type(CommunityBoard.BoardType.valueOf(req.type().toUpperCase()))
                .visibility(CommunityBoard.Visibility.valueOf(req.visibility().toUpperCase()))
                .isSystem(req.system())
                .sortOrder(req.sortOrder() == null ? 0 : req.sortOrder())
                .build();
        return BoardRes.of(service.create(b));
    }

    // 수정(이름/정렬)
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BoardRes update(@PathVariable Long id, @Valid @RequestBody BoardDtos.UpdateBoardReq req) {
        return BoardRes.of(service.update(id, req.name(), req.sortOrder()));
    }

    // 공개범위 변경
    @PatchMapping("/{id}/visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public BoardRes changeVisibility(@PathVariable Long id, @Valid @RequestBody BoardDtos.ChangeVisibilityReq req) {
        var v = CommunityBoard.Visibility.valueOf(req.visibility().toUpperCase());
        return BoardRes.of(service.changeVisibility(id, v));
    }

    // 소프트 삭제(복구는 관리자만 DB에서 visibility 변경/undelete 로직 추가해서 확장 가능)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        service.softDelete(id);
    }
}
