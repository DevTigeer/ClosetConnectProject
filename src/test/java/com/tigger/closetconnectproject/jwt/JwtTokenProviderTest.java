package com.tigger.closetconnectproject.jwt;
import com.tigger.closetconnectproject.Common.Jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 단위 테스트
 * - 토큰 생성 및 파싱 테스트
 * - JWT 예외 처리 테스트
 * - Secret 검증 테스트
 */
class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private static final String TEST_SECRET = "this-is-test-secret-this-is-test-secret-this-is-test-secret";
    private static final long TEST_VALIDITY_SECONDS = 3600L;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        // ReflectionTestUtils를 사용하여 private 필드 주입
        ReflectionTestUtils.setField(provider, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(provider, "validitySeconds", TEST_VALIDITY_SECONDS);
        provider.init();
    }

    @Test
    @DisplayName("토큰을 생성하고 다시 파싱할 수 있다")
    void createAndParse() {
        // Given
        String email = "u@cc.com";
        Map<String, Object> claims = Map.of("uid", 1L, "role", "ROLE_USER");

        // When
        String token = provider.createToken(email, claims);
        Claims parsedClaims = provider.parse(token).getBody();

        // Then
        assertThat(parsedClaims.getSubject()).isEqualTo(email);
        assertThat(parsedClaims.get("uid", Number.class).longValue()).isEqualTo(1L);
        assertThat(parsedClaims.get("role", String.class)).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("getSubject로 이메일을 추출할 수 있다")
    void getSubject() {
        // Given
        String email = "test@test.com";
        String token = provider.createToken(email, Map.of("uid", 99L));

        // When
        String subject = provider.getSubject(token);

        // Then
        assertThat(subject).isEqualTo(email);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰은 JwtValidationException을 발생시킨다")
    void invalidTokenFormat() {
        // Given
        String invalidToken = "invalid.token.format";

        // When & Then
        assertThatThrownBy(() -> provider.parse(invalidToken))
                .isInstanceOf(JwtTokenProvider.JwtValidationException.class)
                .hasMessageContaining("형식");
    }

    @Test
    @DisplayName("빈 토큰은 JwtValidationException을 발생시킨다")
    void emptyToken() {
        // Given
        String emptyToken = "";

        // When & Then
        assertThatThrownBy(() -> provider.parse(emptyToken))
                .isInstanceOf(JwtTokenProvider.JwtValidationException.class)
                .hasMessageContaining("비어있습니다");
    }

    @Test
    @DisplayName("다른 secret으로 생성된 토큰은 SignatureException을 발생시킨다")
    void invalidSignature() {
        // Given - 다른 secret으로 provider 생성
        JwtTokenProvider anotherProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(anotherProvider, "secret", "different-secret-key-different-secret-key-different");
        ReflectionTestUtils.setField(anotherProvider, "validitySeconds", 3600L);
        anotherProvider.init();

        String tokenFromAnotherProvider = anotherProvider.createToken("test@test.com", Map.of());

        // When & Then - 원래 provider로 파싱 시도
        assertThatThrownBy(() -> provider.parse(tokenFromAnotherProvider))
                .isInstanceOf(JwtTokenProvider.JwtValidationException.class)
                .hasMessageContaining("서명");
    }

    @Test
    @DisplayName("만료된 토큰은 JwtValidationException을 발생시킨다")
    void expiredToken() throws InterruptedException {
        // Given - 만료 시간이 매우 짧은 provider
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortLivedProvider, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(shortLivedProvider, "validitySeconds", 1L); // 1초
        shortLivedProvider.init();

        String token = shortLivedProvider.createToken("test@test.com", Map.of("uid", 1L));

        // When - 2초 대기
        Thread.sleep(2000);

        // Then
        assertThatThrownBy(() -> shortLivedProvider.parse(token))
                .isInstanceOf(JwtTokenProvider.JwtValidationException.class)
                .hasMessageContaining("만료");
    }

    @Test
    @DisplayName("secret이 최소 길이보다 짧으면 IllegalArgumentException을 발생시킨다")
    void secretTooShort() {
        // Given
        JwtTokenProvider newProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(newProvider, "secret", "short"); // 너무 짧은 secret
        ReflectionTestUtils.setField(newProvider, "validitySeconds", 3600L);

        // When & Then
        assertThatThrownBy(newProvider::init)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소 32자");
    }

    @Test
    @DisplayName("secret이 비어있으면 IllegalArgumentException을 발생시킨다")
    void secretEmpty() {
        // Given
        JwtTokenProvider newProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(newProvider, "secret", "");
        ReflectionTestUtils.setField(newProvider, "validitySeconds", 3600L);

        // When & Then
        assertThatThrownBy(newProvider::init)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("설정되지 않았습니다");
    }

    @Test
    @DisplayName("여러 클레임을 포함한 토큰을 생성하고 파싱할 수 있다")
    void multipleClaimsToken() {
        // Given
        String email = "admin@test.com";
        Map<String, Object> claims = Map.of(
                "uid", 999L,
                "role", "ROLE_ADMIN",
                "nickname", "관리자"
        );

        // When
        String token = provider.createToken(email, claims);
        Claims parsedClaims = provider.parse(token).getBody();

        // Then
        assertThat(parsedClaims.getSubject()).isEqualTo(email);
        assertThat(parsedClaims.get("uid", Number.class).longValue()).isEqualTo(999L);
        assertThat(parsedClaims.get("role", String.class)).isEqualTo("ROLE_ADMIN");
        assertThat(parsedClaims.get("nickname", String.class)).isEqualTo("관리자");
    }
}
