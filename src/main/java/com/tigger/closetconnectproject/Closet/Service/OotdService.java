package com.tigger.closetconnectproject.Closet.Service;

import com.tigger.closetconnectproject.Closet.Dto.OotdDtos;
import com.tigger.closetconnectproject.Closet.Entity.Ootd;
import com.tigger.closetconnectproject.Closet.Repository.OotdRepository;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OotdService {

    private final OotdRepository ootdRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public OotdDtos.Response save(Long userId, OotdDtos.CreateRequest request) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Ootd ootd = Ootd.builder()
                .user(user)
                .imageUrl(request.imageUrl())
                .description(request.description())
                .build();

        Ootd saved = ootdRepository.save(ootd);
        return OotdDtos.Response.from(saved);
    }

    public List<OotdDtos.Response> findByUserId(Long userId) {
        return ootdRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(OotdDtos.Response::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long ootdId, Long userId) {
        Ootd ootd = ootdRepository.findById(ootdId)
                .orElseThrow(() -> new IllegalArgumentException("OOTD를 찾을 수 없습니다."));

        if (!ootd.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        ootdRepository.delete(ootd);
    }
}
