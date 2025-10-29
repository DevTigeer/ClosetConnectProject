package com.tigger.Common.Auth;

import com.tigger.User.Dto.LoginRequest;
import com.tigger.User.Dto.SignUpRequest;
import com.tigger.User.Dto.TokenResponse;
import com.tigger.User.Dto.UserSummary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserSummary> signUp(@RequestBody @Valid SignUpRequest req) {
        return ResponseEntity.ok(authService.signUp(req));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
}
