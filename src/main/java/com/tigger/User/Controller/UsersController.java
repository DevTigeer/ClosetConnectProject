package com.tigger.User.Controller;

import com.tigger.Common.Auth.AuthService;
import com.tigger.Common.Security.AppUserDetails;
import com.tigger.User.Dto.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UsersController {

    private final AuthService authService;

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal AppUserDetails principal) {
        return authService.me(principal.getUsername()); // email 기반
    }
}

