package com.tigger.closetconnectproject.Common.Auth;

import com.tigger.closetconnectproject.User.Dto.LoginRequest;
import com.tigger.closetconnectproject.User.Dto.SignUpRequest;
import com.tigger.closetconnectproject.User.Dto.TokenResponse;
import com.tigger.closetconnectproject.User.Dto.UserSummary;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserSummary> signUp(@RequestBody @Valid SignUpRequest req) {
        UserSummary user = authService.signUp(req);
        URI location = URI.create("/api/v1/users/" + user.id());

        return ResponseEntity.created(location).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
}
