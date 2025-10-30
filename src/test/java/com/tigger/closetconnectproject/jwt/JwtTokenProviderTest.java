package com.tigger.closetconnectproject.jwt;
import com.tigger.closetconnectproject.Common.Jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    @Test
    @DisplayName("토큰을 생성하고 다시 파싱할 수 있다.")
    void createAndParse() {
        JwtTokenProvider provider = new JwtTokenProvider();
        // 수동 주입
        provider.secret = "this-is-test-secret-this-is-test-secret-this-is-test";
        provider.validitySeconds = 3600L;
        provider.init();

        String token = provider.createToken("u@cc.com",
                Map.of("uid", 1L, "role", "ROLE_USER"));

        Claims claims = provider.parse(token).getBody();

        assertThat(claims.getSubject()).isEqualTo("u@cc.com");
        assertThat(claims.get("uid", Number.class).longValue()).isEqualTo(1L);
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_USER");
    }
}
