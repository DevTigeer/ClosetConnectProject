package com.tigger.closetconnectproject.User.View;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClosetViewController {

    @GetMapping("/closet")
    public String closet(Model model, Authentication authentication) {
        boolean isLogin = authentication != null && authentication.isAuthenticated();
        model.addAttribute("isLogin", isLogin);
        return "closet";
    }

}
