package com.tigger.closetconnectproject.Common.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Swagger/OpenAPI 문서 설정
 * - Swagger UI: /swagger-ui/index.html
 * - OpenAPI JSON: /v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    @Value("${swagger.server.url:http://localhost:8080}")
    private String serverUrl;

    @Value("${swagger.server.description:Local Development Server}")
    private String serverDescription;

    @Bean
    public OpenAPI openAPI() {
        // JWT 인증 스키마 이름
        String securitySchemeName = "Bearer Authentication";

        // 서버 정보 설정 (환경변수로 주입 가능)
        List<Server> servers = new ArrayList<>();
        servers.add(new Server()
                .url(serverUrl)
                .description(serverDescription));

        // 로컬 개발 서버 추가 (프로덕션 환경에서도 참고용으로 표시)
        if (!serverUrl.contains("localhost")) {
            servers.add(new Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server"));
        }

        return new OpenAPI()
                // API 기본 정보
                .info(new Info()
                        .title("ClosetConnect API")
                        .description("""
                                # ClosetConnect API 문서

                                옷장 관리, AI 기반 코디 추천, 커뮤니티, 마켓 기능을 제공하는 REST API입니다.

                                ## 주요 기능
                                - **옷장 관리**: 옷 등록/수정/삭제, AI 배경 제거 및 확장
                                - **코디 관리**: 코디 생성/관리, OOTD 공유
                                - **커뮤니티**: 게시판, 댓글, 좋아요
                                - **마켓**: 중고 거래, 채팅, 결제 (토스페이먼츠)
                                - **날씨 기반 추천**: 날씨에 맞는 코디 추천

                                ## 인증 방법
                                1. `/api/v1/auth/login` 엔드포인트로 로그인하여 JWT 토큰 발급
                                2. 우측 상단 "Authorize" 버튼 클릭
                                3. 발급받은 토큰을 입력 (Bearer 접두사 없이)
                                4. 이후 모든 API 요청에 자동으로 토큰이 포함됨

                                ## API 버전
                                - 현재 버전: v1.0.0
                                - Base Path: /api/v1
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("ClosetConnect Team")
                                .email("contact@closetconnect.com")
                                .url("https://github.com/your-repo/closetconnect"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))

                // 서버 정보
                .servers(servers)

                // JWT 인증 설정 (모든 API에 기본 적용)
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                                        JWT 토큰을 입력하세요.

                                        'Bearer ' 접두사는 자동으로 추가되므로, 토큰 값만 입력하면 됩니다.

                                        예시:
                                        - ❌ Bearer eyJhbGciOiJIUzI1NiJ9...
                                        - ✅ eyJhbGciOiJIUzI1NiJ9...
                                        """)));
    }
}
