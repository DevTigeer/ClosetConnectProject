package com.tigger.closetconnectproject.Common.Jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * JWT 토큰 생성 및 검증을 담당하는 컴포넌트
 * HS256 알고리즘을 사용하여 토큰을 서명하고 검증합니다.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final int MINIMUM_SECRET_LENGTH = 32;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-validity-seconds}")
    private long validitySeconds;

    private Key key;

    /**
     * JWT 서명 키를 초기화합니다.
     * 애플리케이션 시작 시 secret 값의 유효성을 검증합니다.
     *
     * @throws IllegalArgumentException secret이 최소 길이보다 짧을 경우
     */
    @PostConstruct
    public void init() {
        validateSecret();
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT TokenProvider 초기화 완료 - 토큰 유효 시간: {}초", validitySeconds);
    }

    /**
     * JWT secret의 유효성을 검증합니다.
     */
    private void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret이 설정되지 않았습니다.");
        }
        if (secret.length() < MINIMUM_SECRET_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("JWT secret은 최소 %d자 이상이어야 합니다. 현재: %d자",
                            MINIMUM_SECRET_LENGTH, secret.length())
            );
        }
    }

    /**
     * JWT 토큰을 생성합니다.
     *
     * @param subject 토큰의 주체 (일반적으로 사용자 이메일)
     * @param claims  추가 클레임 (role, userId 등)
     * @return 생성된 JWT 토큰 문자열
     */
    public String createToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + validitySeconds * 1000);

        return Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * JWT 토큰을 파싱하고 검증합니다.
     *
     * @param token 검증할 JWT 토큰
     * @return 파싱된 JWT Claims
     * @throws JwtValidationException 토큰이 유효하지 않을 경우
     */
    public Jws<Claims> parse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
            throw new JwtValidationException("토큰이 만료되었습니다.", e);
        } catch (SignatureException e) {
            log.warn("잘못된 JWT 서명: {}", e.getMessage());
            throw new JwtValidationException("토큰 서명이 유효하지 않습니다.", e);
        } catch (MalformedJwtException e) {
            log.warn("잘못된 형식의 JWT 토큰: {}", e.getMessage());
            throw new JwtValidationException("토큰 형식이 올바르지 않습니다.", e);
        } catch (UnsupportedJwtException e) {
            log.warn("지원하지 않는 JWT 토큰: {}", e.getMessage());
            throw new JwtValidationException("지원하지 않는 토큰 형식입니다.", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있음: {}", e.getMessage());
            throw new JwtValidationException("토큰이 비어있습니다.", e);
        }
    }

    /**
     * JWT 토큰에서 subject (이메일)을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 토큰의 subject
     * @throws JwtValidationException 토큰이 유효하지 않을 경우
     */
    public String getSubject(String token) {
        return parse(token).getBody().getSubject();
    }

    /**
     * JWT 토큰 검증 실패 시 발생하는 예외
     */
    public static class JwtValidationException extends RuntimeException {
        public JwtValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

