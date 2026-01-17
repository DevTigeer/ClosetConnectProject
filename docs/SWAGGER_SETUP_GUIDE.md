# Swagger API 문서 설정 가이드

## 📋 목차
1. [현재 상태](#현재-상태)
2. [SpringDoc OpenAPI 설정](#springdoc-openapi-설정)
3. [의존성 추가](#의존성-추가)
4. [설정 클래스 작성](#설정-클래스-작성)
5. [JWT 인증 설정](#jwt-인증-설정)
6. [컨트롤러 문서화](#컨트롤러-문서화)
7. [접근 및 테스트](#접근-및-테스트)

---

## 현재 상태

### ❌ 설정되지 않은 항목
- Swagger 의존성 없음
- OpenAPI 설정 클래스 없음
- API 문서화 어노테이션 없음

### ✅ 준비된 항목
- SecurityConfig에 Swagger 경로 허용 설정됨 (line 39)
  ```java
  .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
  ```

---

## SpringDoc OpenAPI 설정

Spring Boot 3.x 에서는 **SpringDoc OpenAPI 3**을 사용합니다.

---

## 의존성 추가

### build.gradle 수정

`dependencies` 블록에 다음을 추가:

```gradle
dependencies {
    // 기존 의존성들...

    // Swagger/OpenAPI 문서화
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
}
```

---

## 설정 클래스 작성

### src/main/java/com/tigger/closetconnectproject/Common/Config/SwaggerConfig.java

```java
package com.tigger.closetconnectproject.Common.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // JWT 인증 스키마 이름
        String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                // API 기본 정보
                .info(new Info()
                        .title("ClosetConnect API")
                        .description("옷장 관리, 코디 추천, 커뮤니티, 마켓 기능을 제공하는 API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("ClosetConnect Team")
                                .email("contact@closetconnect.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))

                // 서버 정보
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("로컬 개발 서버"),
                        new Server()
                                .url("https://your-production-url.com")
                                .description("프로덕션 서버")
                ))

                // JWT 인증 설정
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다.")));
    }
}
```

---

## JWT 인증 설정

### Swagger UI에서 JWT 토큰 사용하기

1. **로그인 API로 토큰 발급**
   - `POST /api/v1/auth/login` 실행
   - Response에서 `accessToken` 복사

2. **Authorize 버튼 클릭**
   - Swagger UI 우측 상단 "Authorize" 버튼 클릭
   - 토큰 입력 (Bearer 접두사 없이 토큰만 입력)
   - "Authorize" 버튼 클릭

3. **인증 필요한 API 테스트**
   - 이제 모든 API 요청에 자동으로 JWT 토큰이 포함됨

---

## 컨트롤러 문서화

### 기본 어노테이션

```java
package com.tigger.closetconnectproject.Closet.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/cloth")
@RequiredArgsConstructor
@Tag(name = "Cloth", description = "옷장 관리 API")
public class ClothController {

    private final ClothService clothService;

    @Operation(
        summary = "옷 목록 조회",
        description = "사용자의 옷 목록을 조회합니다. 카테고리 필터링, 페이징, 정렬을 지원합니다.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = Page.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패"
        )
    })
    @GetMapping
    public Page<ClothResponse> list(
            @Parameter(description = "카테고리 필터 (TOP, BOTTOM, OUTER, DRESS, ACC)")
            @RequestParam(required = false) Category category,

            @Parameter(description = "페이징 정보 (page, size, sort)")
            Pageable pageable,

            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        return clothService.list(uid, category, pageable);
    }

    @Operation(
        summary = "옷 상세 조회",
        description = "특정 옷 아이템의 상세 정보를 조회합니다.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ClothResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "옷을 찾을 수 없음"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/{id}")
    public ClothResponse detail(
            @Parameter(description = "옷 ID", required = true)
            @PathVariable Long id,

            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        return clothService.detail(uid, id);
    }

    @Operation(
        summary = "옷 등록",
        description = "새로운 옷을 등록합니다. 이미지 파일을 업로드하면 AI가 자동으로 처리합니다.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "등록 성공",
            content = @Content(schema = @Schema(implementation = ClothResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ClothResponse upload(
            @Parameter(description = "이미지 파일", required = true)
            @RequestPart MultipartFile image,

            @Parameter(description = "이미지 타입 (SINGLE_ITEM 또는 FULL_BODY)")
            @RequestParam(defaultValue = "SINGLE_ITEM") String imageType,

            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        return clothService.upload(uid, image, imageType);
    }

    @Operation(
        summary = "옷 수정",
        description = "등록된 옷의 정보를 수정합니다.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "404", description = "옷을 찾을 수 없음"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PutMapping("/{id}")
    public ClothResponse update(
            @Parameter(description = "옷 ID", required = true)
            @PathVariable Long id,

            @Parameter(description = "수정할 옷 정보", required = true)
            @RequestBody @Valid ClothUpdateRequest request,

            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        return clothService.update(uid, id, request);
    }

    @Operation(
        summary = "옷 삭제",
        description = "등록된 옷을 삭제합니다.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "404", description = "옷을 찾을 수 없음"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "옷 ID", required = true)
            @PathVariable Long id,

            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Long uid = principal.getUser().getUserId();
        clothService.delete(uid, id);
    }
}
```

### 주요 어노테이션 설명

| 어노테이션 | 설명 | 사용 위치 |
|-----------|------|----------|
| `@Tag` | API 그룹 정의 | 클래스 레벨 |
| `@Operation` | API 엔드포인트 설명 | 메서드 레벨 |
| `@ApiResponses` | 응답 코드별 설명 | 메서드 레벨 |
| `@Parameter` | 파라미터 설명 | 파라미터 레벨 |
| `@Schema` | DTO/Entity 스키마 정의 | 클래스/필드 레벨 |
| `@SecurityRequirement` | 인증 필요 표시 | 메서드 레벨 |

### DTO 문서화 예시

```java
package com.tigger.closetconnectproject.Closet.Dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "옷 수정 요청")
public class ClothUpdateRequest {

    @Schema(
        description = "카테고리",
        example = "TOP",
        allowableValues = {"TOP", "BOTTOM", "OUTER", "DRESS", "ACC"}
    )
    private Category category;

    @Schema(
        description = "색상",
        example = "Blue"
    )
    private String color;

    @Schema(
        description = "브랜드",
        example = "Nike"
    )
    private String brand;

    @Schema(
        description = "계절",
        example = "SPRING",
        allowableValues = {"SPRING", "SUMMER", "FALL", "WINTER"}
    )
    private Season season;

    // getters, setters...
}
```

---

## 접근 및 테스트

### 1. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 2. Swagger UI 접속

```
http://localhost:8080/swagger-ui/index.html
```

### 3. OpenAPI JSON 스펙

```
http://localhost:8080/v3/api-docs
```

### 4. 테스트 순서

1. **회원가입**: `POST /api/v1/auth/signup`
2. **로그인**: `POST /api/v1/auth/login` → 토큰 발급
3. **Authorize 클릭**: 토큰 입력
4. **API 테스트**: 인증 필요한 API 자유롭게 테스트

---

## 추가 설정

### application.yml 설정 (선택사항)

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
    display-request-duration: true
    disable-swagger-default-url: true
  show-actuator: false
  default-consumes-media-type: application/json
  default-produces-media-type: application/json
```

---

## 컨트롤러별 문서화 체크리스트

### 완료해야 할 컨트롤러

- [ ] ClothController
- [ ] OutfitController
- [ ] OotdController
- [ ] UsersController
- [ ] CommunityBoardController
- [ ] PostController
- [ ] CommentController
- [ ] MarketProductController
- [ ] PaymentController
- [ ] OrderController
- [ ] ChatController
- [ ] WeatherController

### 각 컨트롤러 작업 항목

1. 클래스에 `@Tag` 추가
2. 각 메서드에 `@Operation` 추가
3. 응답 코드별 `@ApiResponses` 추가
4. 파라미터에 `@Parameter` 설명 추가
5. 인증 필요한 API에 `@SecurityRequirement` 추가

---

## 문제 해결

### Swagger UI가 안 보일 때

1. **의존성 확인**
   ```bash
   ./gradlew dependencies | grep springdoc
   ```

2. **SecurityConfig 확인**
   - `/swagger-ui/**`, `/v3/api-docs/**` 경로가 permitAll인지 확인

3. **포트 확인**
   - application.yml에서 `server.port` 확인

### JWT 인증이 안 될 때

1. **토큰 형식 확인**
   - Bearer 접두사 없이 토큰만 입력했는지 확인

2. **토큰 만료 확인**
   - 토큰이 만료되면 재로그인 필요

3. **SecurityConfig 확인**
   - JwtAuthenticationFilter가 올바르게 등록되었는지 확인

---

## 참고 자료

- [SpringDoc OpenAPI 공식 문서](https://springdoc.org/)
- [OpenAPI 3.0 Specification](https://swagger.io/specification/)
- [Swagger UI 가이드](https://swagger.io/tools/swagger-ui/)

---

## 작성일
2026-01-16
