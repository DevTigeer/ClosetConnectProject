package com.tigger.closetconnectproject.Common.Jwt;

import com.tigger.closetconnectproject.Security.AppUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 토큰 인증 필터
 * - 요청 헤더에서 JWT 토큰을 추출하여 검증
 * - 유효한 토큰이면 SecurityContext에 인증 정보 저장
 * - Spring Security Filter Chain에서 UsernamePasswordAuthenticationFilter 앞에 위치
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AppUserDetailsService userDetailsService;

    /**
     * 설명: 모든 HTTP 요청에 대해 JWT 토큰 검증 수행
     * 1. Authorization 헤더에서 "Bearer {token}" 형식 확인
     * 2. JWT 토큰 파싱 및 검증 (만료, 서명 등)
     * 3. 토큰에서 추출한 email로 사용자 정보 로드
     * 4. SecurityContext에 인증 정보 설정
     * 5. 토큰이 없거나 유효하지 않으면 그냥 통과 (401은 Controller에서 처리)
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Authorization 헤더에서 JWT 토큰 추출
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7); // "Bearer " 제거
            try {
                // JWT 파싱 및 검증 (만료, 서명 체크)
                Claims claims = jwtTokenProvider.parse(token).getBody();
                String email = claims.getSubject(); // JWT subject에 저장된 email

                // DB에서 사용자 정보 로드
                var userDetails = userDetailsService.loadUserByUsername(email);

                // Spring Security 인증 객체 생성
                var auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // 토큰 파싱 실패, 만료, 서명 오류 등 → 인증 실패 처리
                // SecurityContext를 비우고 다음 필터로 진행 (최종적으로 401 반환)
                SecurityContextHolder.clearContext();
            }
        }
        // 다음 필터로 진행
        chain.doFilter(request, response);
    }
}

