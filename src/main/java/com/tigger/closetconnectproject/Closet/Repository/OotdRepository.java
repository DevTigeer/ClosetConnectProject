package com.tigger.closetconnectproject.Closet.Repository;

import com.tigger.closetconnectproject.Closet.Entity.Ootd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OotdRepository extends JpaRepository<Ootd, Long> {
    List<Ootd> findByUserUserIdOrderByCreatedAtDesc(Long userId);
}
