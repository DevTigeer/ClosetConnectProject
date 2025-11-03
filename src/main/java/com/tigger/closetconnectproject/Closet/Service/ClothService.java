package com.tigger.closetconnectproject.Closet.Service;

import com.tigger.closetconnectproject.Closet.Dto.ClothCreateRequest;
import com.tigger.closetconnectproject.Closet.Dto.ClothResponse;
import com.tigger.closetconnectproject.Closet.Entity.Category;
import com.tigger.closetconnectproject.Closet.Entity.Cloth;
import com.tigger.closetconnectproject.Closet.Repository.ClothRepository;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClothService {

    private final ClothRepository clothRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public ClothResponse create(Long userId, ClothCreateRequest req) {
        Users owner = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        Cloth saved = clothRepository.save(
                Cloth.builder()
                        .user(owner)
                        .name(req.name())
                        .category(req.category())
                        .color(req.color())
                        .brand(req.brand())
                        .imageUrl(req.imageUrl())
                        .build()
        );
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<ClothResponse> list(Long userId, Category category, Pageable pageable) {
        Page<Cloth> page = (category == null)
                ? clothRepository.findByUser_UserId(userId, pageable)
                : clothRepository.findByUser_UserIdAndCategory(userId, category, pageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ClothResponse getOne(Long userId, Long clothId) {
        Cloth c = clothRepository.findById(clothId)
                .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다."));
        if (!c.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("본인 소유가 아닙니다.");
        }
        return toDto(c);
    }

    @Transactional
    public void delete(Long userId, Long clothId) {
        Cloth c = clothRepository.findById(clothId)
                .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다."));
        if (!c.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("본인 소유가 아닙니다.");
        }
        clothRepository.delete(c);
    }

    private ClothResponse toDto(Cloth c) {
        return new ClothResponse(
                c.getId(), c.getName(), c.getCategory(), c.getColor(), c.getBrand(), c.getImageUrl()
        );
    }
}

