package com.tigger.closetconnectproject.User.Service;

import com.tigger.closetconnectproject.User.Dto.UserSummary;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {


    private final UsersRepository usersRepository;

    public UserSummary getUserById(Long id) {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return new UserSummary(user.getUserId(), user.getEmail(), user.getNickname(), user.getRole().name());
    }

    public UserSummary getUserByEmail(String email) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return new UserSummary(user.getUserId(), user.getEmail(), user.getNickname(), user.getRole().name());
    }
}
