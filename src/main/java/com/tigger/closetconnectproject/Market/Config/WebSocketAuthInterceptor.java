package com.tigger.closetconnectproject.Market.Config;

import com.tigger.closetconnectproject.Common.Jwt.JwtTokenProvider;
import com.tigger.closetconnectproject.Security.AppUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * WebSocket 메시지 채널 인터셉터 - JWT 인증 처리
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final AppUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // CONNECT 프레임에서 Authorization 헤더 추출
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    // JWT 토큰에서 이메일 추출
                    String email = jwtTokenProvider.getSubject(token);

                    // 사용자 정보 로드
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    // 인증 객체 생성 및 설정
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    accessor.setUser(authentication);
                } catch (Exception e) {
                    // JWT 파싱 실패 시 인증 없이 진행 (컨트롤러에서 처리)
                    System.err.println("WebSocket JWT authentication failed: " + e.getMessage());
                }
            }
        }

        return message;
    }
}
