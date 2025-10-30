package com.tigger.closetconnectproject.User.View;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    // 메인 옷장 페이지
    @GetMapping("/closet")
    public String closet(Model model, Authentication authentication) {
        boolean isLogin = authentication != null && authentication.isAuthenticated();
        model.addAttribute("isLogin", isLogin);
        return "closet";
    }

    // 로그인 폼
    @GetMapping("/auth/login")
    public String loginPage() {
        return "auth/login";
    }

    // 회원가입 폼
    @GetMapping("/auth/signup")
    public String signupPage() {
        return "auth/signup";
    }

    // 내 정보 보기
    @GetMapping("/users/me")
    public String mePage(Model model, Authentication authentication) {
        // 나중에 여기서 /users/me API를 서버에서 한 번 더 호출하거나
        // authentication principal 꺼내서 보여주면 됨
        boolean isLogin = authentication != null && authentication.isAuthenticated();
        model.addAttribute("isLogin", isLogin);
        // 우선 임시값
        model.addAttribute("email", isLogin ? authentication.getName() : null);
        return "users/me";
    }
}
