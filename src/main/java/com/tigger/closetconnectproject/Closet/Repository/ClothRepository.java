package com.tigger.closetconnectproject.Closet.Repository;

import com.tigger.closetconnectproject.Closet.Entity.Category;
import com.tigger.closetconnectproject.Closet.Entity.Cloth;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothRepository extends JpaRepository<Cloth, Long> {
    Page<Cloth> findByUser_UserId(Long userId, Pageable pageable);
    Page<Cloth> findByUser_UserIdAndCategory(Long userId, Category category, Pageable pageable);
}

