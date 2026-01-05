        # CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ClosetConnect** is a Spring Boot REST API application for managing a fashion closet and community platform. Users can manage their clothing items, create posts/boards, comment, and interact through likes. The application uses JWT authentication and Spring Security.

## Technology Stack

- **Framework**: Spring Boot 3.5.7
- **Language**: Java 17
- **Build Tool**: Gradle
- **Database**: MariaDB
- **ORM**: Spring Data JPA / Hibernate
- **Security**: Spring Security + JWT (using jjwt 0.11.5)
- **Testing**: JUnit 5 + Spring Security Test + MockMvc

## Build and Run Commands

### Development
```bash
# Run the application
./gradlew bootRun

# Build the project
./gradlew build

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.tigger.closetconnectproject.Post.PostControllerTest"

# Run a specific test method
./gradlew test --tests "com.tigger.closetconnectproject.Post.PostControllerTest.testCreatePost"

# Clean build artifacts
./gradlew clean
```

### Database Setup
The application expects MariaDB running on `localhost:3306` with:
- Database: `closetConnectProject`
- Username: `root`
- Password: `root1234`

DDL auto-mode is set to `update`, so tables will be created/updated automatically.

## Architecture

### Package Structure

The codebase follows a domain-driven vertical slice architecture under `com.tigger.closetconnectproject`:

- **Closet**: Clothing item management (cloth CRUD operations)
- **Community**: Community board posts and interactions
- **Post**: Post system with comments, likes, attachments, and reporting
- **User**: User management and profiles
- **Upload**: File upload handling
- **Weather**: Weather-related functionality
- **Common**: Shared components
  - `Auth/`: Authentication (signup, login)
  - `Jwt/`: JWT token provider and authentication filter
  - `Config/`: Web configuration (static resource handling)
  - `Entity/`: Base entities (e.g., `BaseTimeEntity` for audit timestamps)
  - `Exception/`: Global exception handling
- **Security**: Security configuration (moved from Common/Security)

Each domain package typically contains:
- `Controller/`: REST controllers
- `Service/`: Business logic
- `Repository/`: JPA repositories
- `Entity/`: JPA entities
- `Dto/`: Data transfer objects

### Authentication & Authorization

**JWT-based authentication** using a custom filter chain:

1. `JwtTokenProvider` (Common/Jwt/JwtTokenProvider.java): Creates and parses JWT tokens
   - Secret key configured via `jwt.secret` in application.properties
   - Token validity: `jwt.access-token-validity-seconds` (default 3600s = 1 hour)
   - Subject stores email, claims store role and userId

2. `JwtAuthenticationFilter` (Common/Jwt/JwtAuthenticationFilter.java): Intercepts requests and validates JWT
   - Extracts token from `Authorization: Bearer <token>` header
   - Loads user details via `AppUserDetailsService`
   - Sets authentication in SecurityContext

3. `SecurityConfig` (Security/SecurityConfig.java): Configures Spring Security
   - Stateless sessions (no JSESSIONID)
   - CSRF disabled
   - Public endpoints: `/api/v1/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`
   - Authenticated endpoints: `/users/**`, `/api/v1/cloth/**`, `/api/v1/boards/**`, `/api/v1/posts/**`
   - Admin endpoints: `/api/v1/admin/**` (requires ROLE_ADMIN)
   - CORS enabled for all origins

**User roles** (User/Entity/UserRole.java):
- `ROLE_USER`: Standard user
- `ROLE_ADMIN`: Administrator with access to admin endpoints

### Entity Design

**Base entity** (`Common/Entity/BaseTimeEntity.java`):
- All domain entities extend this for automatic timestamp tracking
- Provides `createdAt` and `updatedAt` fields
- Uses JPA auditing (@CreatedDate, @LastModifiedDate)

**Users entity** (`User/Entity/Users.java`):
- Primary key: `userId` (auto-increment)
- Unique constraints on `email` and `nickname`
- Password stored as BCrypt hash
- Status: NORMAL, SUSPENDED, DELETED
- Role: ROLE_USER, ROLE_ADMIN

### File Upload System

Configured in `application.properties`:
- Upload directory: `./uploads` (relative to project root)
- Max file size: 5MB
- Max request size: 6MB
- Public URL prefix: `/uploads`

Static resources mapped via `WebConfig` to serve uploaded files.

### Testing Patterns

**Controller tests** use `@WebMvcTest` with:
- `@AutoConfigureMockMvc(addFilters = false)`: Disable security filters for URL/JSON testing
- `@MockBean PostService`: Mock service layer
- `@MockBean JpaMetamodelMappingContext`: Mock JPA metamodel
- `@MockBean AuditorAware<Long>`: Mock auditing
- `@WithMockUser`: Simulate authenticated user for method security

Example pattern (see Post/PostControllerTest.java):
```java
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = PostController.class)
class PostControllerTest {
    @Autowired MockMvc mvc;
    @MockBean PostService postService;

    @Test
    @WithMockUser
    void testEndpoint() throws Exception {
        // Mock service behavior
        given(postService.someMethod(...)).willReturn(...);

        // Perform request and verify
        mvc.perform(get("/api/v1/posts/..."))
           .andExpect(status().isOk());
    }
}
```

## Common Patterns

### DTO Naming Convention
DTOs are grouped in nested static classes within `*Dtos.java` files (e.g., `PostDtos.java`, `CommentDtos.java`):
```java
public class PostDtos {
    public record CreateRequest(...) {}
    public record UpdateRequest(...) {}
    public record Response(...) {}
}
```

### Request/Response Flow
1. Controller receives request with @Valid DTO
2. Controller extracts authenticated user from SecurityContext
3. Service performs business logic
4. Service returns DTO response
5. Controller wraps in ResponseEntity

### Security Context Access
To get the current authenticated user:
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
AppUserDetails userDetails = (AppUserDetails) auth.getPrincipal();
Long userId = userDetails.getUserId();
```

## API Endpoint Patterns

- `/api/v1/auth/signup` - User registration
- `/api/v1/auth/login` - User login (returns JWT token)
- `/api/v1/posts/**` - Post operations (CRUD, comments, likes)
- `/api/v1/boards/**` - Community boards
- `/api/v1/cloth/**` - Clothing item management
- `/api/v1/admin/**` - Admin operations (post/board moderation, user management)

## Important Notes

- The project has recent refactoring moving Security classes from `Common/Security/` to `Security/` (as seen in git status)
- Upload directory `./uploads` is gitignored but must exist at runtime
- JWT secret in application.properties should be changed for production
- Database credentials in application.properties are development defaults

### notice

- md파일로 요약하지말것
