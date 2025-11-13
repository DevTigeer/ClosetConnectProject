package com.tigger.closetconnectproject.Community.Service;

import com.tigger.closetconnectproject.Community.Entity.CommunityBoard;
import com.tigger.closetconnectproject.Community.Repository.CommunityBoardRepository;

import com.tigger.closetconnectproject.Community.Entity.CommunityBoard.Visibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityBoardService {

    private final CommunityBoardRepository repo;

    @Transactional(readOnly = true)
    public List<CommunityBoard> listPublic() {
        return repo.findAllByVisibilityAndDeletedAtIsNullOrderBySortOrderAscIdAsc(Visibility.PUBLIC);
    }

    @Transactional(readOnly = true)
    public List<CommunityBoard> listAllForAdmin() {
        return repo.findAllByDeletedAtIsNullOrderBySortOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public CommunityBoard getBySlug(String slug) {
        return repo.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new IllegalArgumentException("board not found: " + slug));
    }

    @Transactional
    public CommunityBoard create(CommunityBoard b) {
        // slug 중복/삭제여부 검증은 DB unique + deletedAt null 조건으로 커버
        return repo.save(b);
    }

    @Transactional
    public CommunityBoard update(Long id, String name, Integer sortOrder) {
        CommunityBoard b = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("board not found"));
        if (b.getDeletedAt() != null) throw new IllegalStateException("board deleted");
        if (name != null) b.setName(name);
        if (sortOrder != null) b.setSortOrder(sortOrder);
        return b;
    }

    @Transactional
    public CommunityBoard changeVisibility(Long id, CommunityBoard.Visibility v) {
        CommunityBoard b = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("board not found"));
        if (b.getDeletedAt() != null) throw new IllegalStateException("board deleted");
        b.setVisibility(v);
        return b;
    }

    @Transactional
    public void softDelete(Long id) {
        CommunityBoard b = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("board not found"));
        if (b.getDeletedAt() != null) return;
        b.setDeletedAt(LocalDateTime.now());
    }
}

