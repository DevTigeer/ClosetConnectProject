package com.tigger.closetconnectproject.Closet.Repository;

import com.tigger.closetconnectproject.Closet.Entity.Category;
import com.tigger.closetconnectproject.Closet.Entity.Cloth;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothRepository extends JpaRepository<Cloth, Long> {
    Page<Cloth> findByUser_UserId(Long userId, Pageable pageable);
    Page<Cloth> findByUser_UserIdAndCategory(Long userId, Category category, Pageable pageable);

    // confirmed=true인 옷만 조회 (사용자가 최종 이미지를 선택한 것만)
    Page<Cloth> findByUser_UserIdAndConfirmedTrue(Long userId, Pageable pageable);
    Page<Cloth> findByUser_UserIdAndCategoryAndConfirmedTrue(Long userId, Category category, Pageable pageable);
}

