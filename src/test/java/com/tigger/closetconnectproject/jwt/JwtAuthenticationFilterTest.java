package com.tigger.closetconnectproject.jwt;

import com.tigger.closetconnectproject.Common.Jwt.JwtAuthenticationFilter;
import com.tigger.closetconnectproject.Common.Jwt.JwtTokenProvider;
import com.tigger.closetconnectproject.Security.AppUserDetails;
import com.tigger.closetconnectproject.Security.AppUserDetailsService;
import com.tigger.closetconnectproject.User.Entity.UserRole;
import com.tigger.closetconnectproject.User.Entity.UserStatus;
import com.tigger.closetconnectproject.User.Entity.Users;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter 단위 테스트
 * - JWT 토큰 추출 및 검증 테스트
 * - SecurityContext 설정 테스트
 * - 예외 처리 테스트
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AppUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Jws<Claims> mockJws;

    @Mock
    private Claims mockClaims;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Users testUser;
    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        testUser = Users.builder()
                .userId(1L)
                .email("test@test.com")
                .password("encoded")
                .nickname("테스터")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();

        testUserDetails = new AppUserDetails(testUser);

        // SecurityContext 초기화
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 인증이 설정된다")
    void validJwtToken_setsAuthentication() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validToken);

        given(mockJws.getBody()).willReturn(mockClaims);
        given(mockClaims.getSubject()).willReturn("test@test.com");
        given(jwtTokenProvider.parse(validToken)).willReturn(mockJws);
        given(userDetailsService.loadUserByUsername("test@test.com")).willReturn(testUserDetails);

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(AppUserDetails.class);
        assertThat(((AppUserDetails) auth.getPrincipal()).getUsername()).isEqualTo("test@test.com");

        verify(jwtTokenProvider).parse(validToken);
        verify(userDetailsService).loadUserByUsername("test@test.com");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증이 설정되지 않는다")
    void noAuthorizationHeader_noAuthentication() throws ServletException, IOException {
        // Given - Authorization 헤더 없음

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(jwtTokenProvider, never()).parse(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 형식이 아닌 토큰은 인증이 설정되지 않는다")
    void nonBearerToken_noAuthentication() throws ServletException, IOException {
        // Given
        request.addHeader("Authorization", "Basic some-token");

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(jwtTokenProvider, never()).parse(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("빈 Authorization 헤더는 인증이 설정되지 않는다")
    void emptyAuthorizationHeader_noAuthentication() throws ServletException, IOException {
        // Given
        request.addHeader("Authorization", "");

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(jwtTokenProvider, never()).parse(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT 검증 실패 시 SecurityContext가 클리어된다")
    void jwtValidationFails_clearsSecurityContext() throws ServletException, IOException {
        // Given
        String invalidToken = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + invalidToken);

        given(jwtTokenProvider.parse(invalidToken))
                .willThrow(new JwtTokenProvider.JwtValidationException("토큰이 만료되었습니다.", null));

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(jwtTokenProvider).parse(invalidToken);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("사용자 정보 로드 실패 시 SecurityContext가 클리어된다")
    void userDetailsLoadFails_clearsSecurityContext() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validToken);

        given(mockJws.getBody()).willReturn(mockClaims);
        given(mockClaims.getSubject()).willReturn("nonexistent@test.com");
        given(jwtTokenProvider.parse(validToken)).willReturn(mockJws);
        given(userDetailsService.loadUserByUsername("nonexistent@test.com"))
                .willThrow(new UsernameNotFoundException("User not found"));

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(jwtTokenProvider).parse(validToken);
        verify(userDetailsService).loadUserByUsername("nonexistent@test.com");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 뒤에 공백만 있으면 빈 토큰으로 파싱 시도한다")
    void bearerWithOnlySpace_parsesEmptyToken() throws ServletException, IOException {
        // Given
        request.addHeader("Authorization", "Bearer ");

        given(jwtTokenProvider.parse(""))
                .willThrow(new JwtTokenProvider.JwtValidationException("토큰이 비어있습니다.", null));

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(jwtTokenProvider).parse("");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("관리자 권한을 가진 JWT 토큰으로 인증이 설정된다")
    void adminJwtToken_setsAuthentication() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validToken);

        Users adminUser = Users.builder()
                .userId(999L)
                .email("admin@test.com")
                .password("encoded")
                .nickname("관리자")
                .role(UserRole.ROLE_ADMIN)
                .status(UserStatus.NORMAL)
                .build();

        given(mockJws.getBody()).willReturn(mockClaims);
        given(mockClaims.getSubject()).willReturn("admin@test.com");
        given(jwtTokenProvider.parse(validToken)).willReturn(mockJws);
        given(userDetailsService.loadUserByUsername("admin@test.com"))
                .willReturn(new AppUserDetails(adminUser));

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        verify(filterChain).doFilter(request, response);
    }
}
