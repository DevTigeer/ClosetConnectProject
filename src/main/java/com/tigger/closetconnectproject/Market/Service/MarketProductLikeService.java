package com.tigger.closetconnectproject.Market.Service;

import com.tigger.closetconnectproject.Market.Dto.MarketProductDtos;
import com.tigger.closetconnectproject.Market.Dto.MarketProductLikeDtos;
import com.tigger.closetconnectproject.Market.Entity.MarketProduct;
import com.tigger.closetconnectproject.Market.Entity.MarketProductImage;
import com.tigger.closetconnectproject.Market.Entity.MarketProductLike;
import com.tigger.closetconnectproject.Market.Repository.MarketProductImageRepository;
import com.tigger.closetconnectproject.Market.Repository.MarketProductLikeRepository;
import com.tigger.closetconnectproject.Market.Repository.MarketProductRepository;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 중고거래 상품 찜/좋아요 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MarketProductLikeService {

    private final MarketProductLikeRepository likeRepo;
    private final MarketProductRepository productRepo;
    private final MarketProductImageRepository imageRepo;
    private final UsersRepository userRepo;

    /**
     * 찜 추가
     */
    public MarketProductLikeDtos.LikeStatusRes addLike(Long productId, Long userId) {
        // 상품 존재 여부 확인
        if (!productRepo.existsById(productId)) {
            throw new IllegalArgumentException("상품을 찾을 수 없습니다.");
        }

        // 중복 찜 방지 (INSERT IGNORE 사용)
        likeRepo.insert(productId, userId);

        // 찜 개수 조회
        long likeCount = likeRepo.countByMarketProduct_Id(productId);

        return MarketProductLikeDtos.LikeStatusRes.builder()
                .liked(true)
                .likeCount(likeCount)
                .build();
    }

    /**
     * 찜 취소
     */
    public MarketProductLikeDtos.LikeStatusRes removeLike(Long productId, Long userId) {
        // 상품 존재 여부 확인
        if (!productRepo.existsById(productId)) {
            throw new IllegalArgumentException("상품을 찾을 수 없습니다.");
        }

        // 찜 삭제
        likeRepo.deleteByMarketProduct_IdAndUser_UserId(productId, userId);

        // 찜 개수 조회
        long likeCount = likeRepo.countByMarketProduct_Id(productId);

        return MarketProductLikeDtos.LikeStatusRes.builder()
                .liked(false)
                .likeCount(likeCount)
                .build();
    }

    /**
     * 찜 상태 토글
     */
    public MarketProductLikeDtos.LikeStatusRes toggleLike(Long productId, Long userId) {
        boolean isLiked = likeRepo.existsByMarketProduct_IdAndUser_UserId(productId, userId);

        if (isLiked) {
            return removeLike(productId, userId);
        } else {
            return addLike(productId, userId);
        }
    }

    /**
     * 사용자가 찜한 상품 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<MarketProductDtos.ProductListRes> getUserLikedProducts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 100));

        Page<MarketProductLike> likes = likeRepo.findByUser_UserIdWithProduct(userId, pageable);

        // 각 상품의 찜 개수 및 썸네일 조회
        Map<Long, Long> likeCountMap = new HashMap<>();
        Map<Long, String> thumbnailMap = new HashMap<>();

        for (MarketProductLike like : likes.getContent()) {
            MarketProduct product = like.getMarketProduct();
            Long productId = product.getId();

            long count = likeRepo.countByMarketProduct_Id(productId);
            likeCountMap.put(productId, count);

            List<MarketProductImage> images = imageRepo.findByMarketProduct_IdOrderByOrderIndexAsc(productId);
            if (!images.isEmpty()) {
                thumbnailMap.put(productId, images.get(0).getImageUrl());
            }
        }

        return likes.map(like -> {
            MarketProduct p = like.getMarketProduct();
            return MarketProductDtos.ProductListRes.of(
                    p,
                    thumbnailMap.get(p.getId()),
                    likeCountMap.getOrDefault(p.getId(), 0L)
            );
        });
    }
}
