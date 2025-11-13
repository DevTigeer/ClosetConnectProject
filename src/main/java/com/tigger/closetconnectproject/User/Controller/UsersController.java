package com.tigger.closetconnectproject.User.Controller;

import com.tigger.closetconnectproject.Common.Auth.AuthService;
import com.tigger.closetconnectproject.Security.AppUserDetails;
import com.tigger.closetconnectproject.User.Dto.UserSummary;
import com.tigger.closetconnectproject.User.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 사용자 정보 조회 API Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UsersController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * 설명: 특정 사용자 정보 조회 (공개 정보)
     * @param id 조회할 사용자 ID
     * @return 사용자 요약 정보 (UserSummary)
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserSummary> getUser(@PathVariable Long id) {
        UserSummary user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * 설명: 현재 로그인한 사용자 자신의 정보 조회
     * @param principal 인증된 사용자 정보 (Spring Security가 자동 주입)
     * @return 현재 사용자의 요약 정보
     */
    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal AppUserDetails principal) {
        // SecurityConfig에서 /users/**는 authenticated() 처리되므로
        // principal == null인 경우는 발생하지 않음
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        return authService.me(principal.getUsername());
    }
}

