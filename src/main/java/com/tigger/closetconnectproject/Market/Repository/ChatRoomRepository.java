package com.tigger.closetconnectproject.Market.Repository;

import com.tigger.closetconnectproject.Market.Entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 채팅방 Repository
 */
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 상품 + 구매자로 채팅방 찾기
     */
    Optional<ChatRoom> findByMarketProduct_IdAndBuyer_UserId(Long productId, Long buyerId);

    /**
     * 사용자가 참여중인 모든 채팅방 조회 (판매자 or 구매자)
     */
    @Query("""
        SELECT r FROM ChatRoom r
        LEFT JOIN FETCH r.marketProduct p
        LEFT JOIN FETCH r.seller
        LEFT JOIN FETCH r.buyer
        WHERE r.seller.userId = :userId OR r.buyer.userId = :userId
        ORDER BY r.lastMessageAt DESC NULLS LAST, r.createdAt DESC
        """)
    List<ChatRoom> findByUserIdWithDetails(@Param("userId") Long userId);

    /**
     * 특정 상품의 모든 채팅방 조회 (판매자용)
     */
    @Query("""
        SELECT r FROM ChatRoom r
        LEFT JOIN FETCH r.buyer
        WHERE r.marketProduct.id = :productId
        ORDER BY r.lastMessageAt DESC NULLS LAST, r.createdAt DESC
        """)
    List<ChatRoom> findByProductIdWithBuyer(@Param("productId") Long productId);

    /**
     * 두 사용자 간의 직접 채팅방 찾기 (상품과 무관한 채팅)
     */
    @Query("""
        SELECT r FROM ChatRoom r
        WHERE r.marketProduct IS NULL
          AND ((r.seller.userId = :user1Id AND r.buyer.userId = :user2Id)
            OR (r.seller.userId = :user2Id AND r.buyer.userId = :user1Id))
        """)
    Optional<ChatRoom> findDirectChatBetweenUsers(@Param("user1Id") Long user1Id,
                                                   @Param("user2Id") Long user2Id);
}
