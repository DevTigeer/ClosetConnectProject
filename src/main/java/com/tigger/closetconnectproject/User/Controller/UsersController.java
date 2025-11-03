package com.tigger.closetconnectproject.User.Controller;

import com.tigger.closetconnectproject.Common.Auth.AuthService;
import com.tigger.closetconnectproject.Common.Security.AppUserDetails;
import com.tigger.closetconnectproject.User.Dto.UserSummary;
import com.tigger.closetconnectproject.User.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UsersController {

    private final AuthService authService;
    private final UserService userService;
    // 1) 방금 생성된 유저 포함, 특정 유저 조회
    @GetMapping("/{id}")
    public ResponseEntity<UserSummary> getUser(@PathVariable Long id) {
        UserSummary user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal AppUserDetails principal) {
        System.out.println("hihi");
        System.out.println(principal.getUsername());
        if (principal == null) {
            // 테스트에서 인증 안 넣거나, 실제 요청에서 토큰 없으면 여기로 들어옴
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        return authService.me(principal.getUsername());
    }
}

