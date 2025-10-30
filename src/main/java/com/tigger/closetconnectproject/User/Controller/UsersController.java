package com.tigger.closetconnectproject.User.Controller;

import com.tigger.closetconnectproject.Common.Auth.AuthService;
import com.tigger.closetconnectproject.Common.Security.AppUserDetails;
import com.tigger.closetconnectproject.User.Dto.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UsersController {

    private final AuthService authService;

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal AppUserDetails principal) {
        System.out.println("hihi");
        System.out.println(principal.getUsername());
        if (principal == null) {
            // 테스트에서 인증 안 넣거나, 실제 요청에서 토큰 없으면 여기로 들어옴
            throw new BadCredentialsException("아이디나 비밀번호가 올바르지 않습니다.");
        }

        return authService.me(principal.getUsername());
    }
}

