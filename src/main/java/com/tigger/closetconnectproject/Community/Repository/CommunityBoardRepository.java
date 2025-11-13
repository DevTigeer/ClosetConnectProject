// src/main/java/com/tigger/closetconnectproject/community/repo/CommunityBoardRepository.java
package com.tigger.closetconnectproject.Community.Repository;

import com.tigger.closetconnectproject.Community.Entity.CommunityBoard;
import com.tigger.closetconnectproject.Community.Entity.CommunityBoard.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommunityBoardRepository extends JpaRepository<CommunityBoard, Long> {
    Optional<CommunityBoard> findBySlugAndDeletedAtIsNull(String slug);
    List<CommunityBoard> findAllByDeletedAtIsNullOrderBySortOrderAscIdAsc();
    List<CommunityBoard> findAllByVisibilityAndDeletedAtIsNullOrderBySortOrderAscIdAsc(Visibility v);
}
