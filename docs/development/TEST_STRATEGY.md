# Test Strategy

> MCP Reference Document for ClosetConnect testing standards, coverage requirements, and quality gates

## 1. Testing Philosophy

### 1.1 Test Pyramid

ClosetConnect follows the testing pyramid approach with a 70-20-10 distribution:

```
        ╱╲
       ╱  ╲  E2E (10%)
      ╱────╲  - User journey tests
     ╱      ╲  - Critical business flows
    ╱────────╲  Integration (20%)
   ╱          ╲  - API contract tests
  ╱────────────╲  - Database integration
 ╱              ╲  - External service mocks
╱────────────────╲ Unit (70%)
                   - Business logic
                   - Utility functions
                   - Domain models
```

**Current Status:**
- Total Tests: 139
- Unit Tests: ~97 (70%)
- Integration Tests: ~28 (20%)
- E2E Tests: ~14 (10%)

### 1.2 Test Categories

| Test Type | Purpose | Scope | Speed | Frequency |
|-----------|---------|-------|-------|-----------|
| **Unit** | Verify single component behavior | Single class/function | <100ms | Every commit |
| **Integration** | Verify component interactions | Multiple classes, DB, message queue | <5s | Every commit |
| **Contract** | Verify API specifications | Controller + DTOs | <2s | Every commit |
| **E2E** | Verify complete user flows | Full stack + external services | <30s | Pre-merge |
| **Performance** | Verify response times, throughput | Full stack under load | Minutes | Daily/weekly |
| **Security** | Verify auth, input validation, XSS | Full stack | <10s | Every commit |
| **AI Model** | Verify model accuracy, output quality | AI services only | Minutes | On model change |

## 2. Unit Testing

### 2.1 Definition

**Unit tests** verify the behavior of a single component (class, function) in isolation from external dependencies.

**Characteristics:**
- No database access (use mocks)
- No network calls (use mocks)
- No file I/O (use in-memory alternatives)
- Deterministic (same input = same output)
- Fast (<100ms per test)

### 2.2 Java Unit Test Example

**Service Layer Test:**
```java
package com.tigger.closetconnectproject.Post.Service;

import com.tigger.closetconnectproject.Post.Entity.Post;
import com.tigger.closetconnectproject.Post.Repository.PostRepository;
import com.tigger.closetconnectproject.User.Entity.Users;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.assertions.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void createPost_ShouldSaveAndReturnPost() {
        // Given
        Long userId = 1L;
        Users user = Users.builder()
            .userId(userId)
            .email("test@test.com")
            .build();

        PostDtos.CreateRequest request = new PostDtos.CreateRequest(
            "Test Title",
            "Test Content",
            null
        );

        Post savedPost = Post.builder()
            .postId(1L)
            .title("Test Title")
            .content("Test Content")
            .author(user)
            .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // When
        PostDtos.Response response = postService.createPost(userId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Test Title");
        assertThat(response.content()).isEqualTo("Test Content");
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void deletePost_WhenNotAuthor_ShouldThrowException() {
        // Given
        Long postId = 1L;
        Long authorId = 1L;
        Long requesterId = 2L;

        Users author = Users.builder().userId(authorId).build();
        Post post = Post.builder()
            .postId(postId)
            .author(author)
            .build();

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // When / Then
        assertThatThrownBy(() -> postService.deletePost(requesterId, postId))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("게시글 작성자만 삭제할 수 있습니다");

        verify(postRepository, never()).delete(any());
    }
}
```

### 2.3 Python Unit Test Example

**AI Service Test:**
```python
# aiModel/tests/test_imagen_service.py
import pytest
from unittest.mock import Mock, patch, MagicMock
from src.services.imagen_service import GoogleAIImagenService

class TestGoogleAIImagenService:
    @pytest.fixture
    def mock_genai(self):
        with patch('src.services.imagen_service.genai') as mock:
            yield mock

    @pytest.fixture
    def service(self, mock_genai):
        return GoogleAIImagenService(api_key="test-api-key")

    def test_expand_image_success(self, service, mock_genai):
        # Given
        mock_model = Mock()
        mock_result = Mock()
        mock_result.images = [Mock(tobytes=lambda: b'fake_image_data')]
        mock_model.generate_images.return_value = mock_result
        mock_genai.ImageGenerationModel.return_value = mock_model

        input_image = Mock()

        # When
        result = service.expand_image(
            input_image=input_image,
            expand_ratio=1.5,
            prompt="Expand this clothing image"
        )

        # Then
        assert result is not None
        assert isinstance(result, bytes)
        mock_model.generate_images.assert_called_once()

    def test_expand_image_invalid_ratio(self, service):
        # When / Then
        with pytest.raises(ValueError, match="Expand ratio must be between"):
            service.expand_image(
                input_image=Mock(),
                expand_ratio=5.0,  # Invalid ratio
                prompt="Test"
            )

    def test_expand_image_api_error(self, service, mock_genai):
        # Given
        mock_model = Mock()
        mock_model.generate_images.side_effect = Exception("API Error")
        mock_genai.ImageGenerationModel.return_value = mock_model

        # When / Then
        with pytest.raises(Exception, match="API Error"):
            service.expand_image(
                input_image=Mock(),
                expand_ratio=1.5,
                prompt="Test"
            )
```

### 2.4 Coverage Requirements

**Minimum Coverage Thresholds:**
- Overall project: 70%
- New code (PRs): 80%
- Critical business logic: 90%
- Utility/helper classes: 60%

**Coverage Measurement:**
```bash
# Java (JaCoCo)
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html

# Python (pytest-cov)
cd aiModel
pytest tests/ --cov=src --cov-report=html
open htmlcov/index.html
```

**Build Failure on Low Coverage:**
```groovy
// build.gradle
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.70
            }
        }
        rule {
            element = 'PACKAGE'
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.60
            }
            excludes = [
                'com.tigger.closetconnectproject.*.Dto.*',
                'com.tigger.closetconnectproject.Common.Config.*'
            ]
        }
    }
}

check.dependsOn jacocoTestCoverageVerification
```

## 3. Integration Testing

### 3.1 Definition

**Integration tests** verify that multiple components work together correctly, including databases, message queues, and internal services.

**Characteristics:**
- Use real databases (H2 in-memory for tests)
- Use real message queues (embedded RabbitMQ or Testcontainers)
- Mock only external services (payment APIs, Google AI)
- Slower than unit tests (<5s acceptable)

### 3.2 Controller Integration Test

**Using @WebMvcTest (lighter weight):**
```java
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = PostController.class)
@MockBean(JpaMetamodelMappingContext.class)
@MockBean(AuditorAware.class)
class PostControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PostService postService;

    @Test
    @WithMockUser
    void createPost_ValidRequest_ShouldReturn201() throws Exception {
        // Given
        PostDtos.CreateRequest request = new PostDtos.CreateRequest(
            "New Post",
            "Content",
            null
        );

        PostDtos.Response response = new PostDtos.Response(
            1L, "New Post", "Content", null, 1L, "author@test.com",
            LocalDateTime.now(), null, 0, 0, false
        );

        given(postService.createPost(anyLong(), any())).willReturn(response);

        // When / Then
        mvc.perform(post("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "New Post",
                        "content": "Content"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("New Post"))
            .andExpect(jsonPath("$.content").value("Content"));
    }
}
```

**Using @SpringBootTest (full integration):**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void fullPostLifecycle_CreateUpdateDelete() throws Exception {
        // 1. Create user
        Users user = userRepository.save(Users.builder()
            .email("test@test.com")
            .password("hashedPassword")
            .nickname("tester")
            .build());

        String token = generateJwtToken(user);

        // 2. Create post
        String createResponse = mvc.perform(post("/api/v1/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Integration Test Post",
                        "content": "This is a full integration test"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long postId = JsonPath.read(createResponse, "$.postId");

        // 3. Update post
        mvc.perform(put("/api/v1/posts/" + postId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Updated Title",
                        "content": "Updated content"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated Title"));

        // 4. Delete post
        mvc.perform(delete("/api/v1/posts/" + postId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        // 5. Verify deletion
        assertThat(postRepository.findById(postId)).isEmpty();
    }
}
```

### 3.3 Database Integration Tests

**Test Configuration:**
```properties
# src/test/resources/application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Disable RabbitMQ for tests
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
```

**Repository Test:**
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClothRepositoryTest {

    @Autowired
    private ClothRepository clothRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUserIdAndStatus_ShouldReturnActiveClothes() {
        // Given
        Users user = entityManager.persist(Users.builder()
            .email("test@test.com")
            .build());

        Cloth activeCloth = entityManager.persist(Cloth.builder()
            .user(user)
            .status(ProcessingStatus.COMPLETED)
            .category(ClothCategory.UPPER)
            .build());

        Cloth processingCloth = entityManager.persist(Cloth.builder()
            .user(user)
            .status(ProcessingStatus.PROCESSING)
            .category(ClothCategory.LOWER)
            .build());

        entityManager.flush();

        // When
        List<Cloth> result = clothRepository.findByUserIdAndStatus(
            user.getUserId(),
            ProcessingStatus.COMPLETED
        );

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClothId()).isEqualTo(activeCloth.getClothId());
    }
}
```

### 3.4 Message Queue Integration Tests

**Using Embedded RabbitMQ:**
```java
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672"
})
class ClothProcessingIntegrationTest {

    @Autowired
    private ClothMessageProducer producer;

    @Autowired
    private ClothRepository clothRepository;

    @Test
    void clothProcessingFlow_ShouldPublishAndConsume() throws Exception {
        // Given
        Cloth cloth = clothRepository.save(Cloth.builder()
            .imagePath("/uploads/test.jpg")
            .category(ClothCategory.UPPER)
            .status(ProcessingStatus.PENDING)
            .build());

        // When
        producer.sendClothProcessingMessage(cloth.getClothId());

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Cloth updated = clothRepository.findById(cloth.getClothId()).orElseThrow();
            assertThat(updated.getStatus()).isIn(
                ProcessingStatus.PROCESSING,
                ProcessingStatus.COMPLETED
            );
        });
    }
}
```

**Python Integration Test with RabbitMQ:**
```python
# aiModel/tests/integration/test_worker.py
import pytest
import pika
import json
from testcontainers.rabbitmq import RabbitMqContainer

@pytest.fixture(scope="module")
def rabbitmq_container():
    with RabbitMqContainer("rabbitmq:3-management") as rabbitmq:
        yield rabbitmq

@pytest.fixture
def rabbitmq_connection(rabbitmq_container):
    params = pika.ConnectionParameters(
        host=rabbitmq_container.get_container_host_ip(),
        port=rabbitmq_container.get_exposed_port(5672)
    )
    connection = pika.BlockingConnection(params)
    yield connection
    connection.close()

def test_worker_processes_message(rabbitmq_connection):
    # Given
    channel = rabbitmq_connection.channel()
    channel.queue_declare(queue='cloth_processing_queue', durable=True)

    message = {
        'clothId': 123,
        'imagePath': '/uploads/test.jpg',
        'imageType': 'SINGLE_ITEM'
    }

    # When
    channel.basic_publish(
        exchange='',
        routing_key='cloth_processing_queue',
        body=json.dumps(message),
        properties=pika.BasicProperties(delivery_mode=2)
    )

    # Then
    # Worker should pick up and process (verify in result queue)
    method, properties, body = channel.basic_get(
        queue='cloth_result_queue',
        auto_ack=True
    )

    result = json.loads(body)
    assert result['clothId'] == 123
    assert result['status'] == 'COMPLETED'
```

## 4. External Service Mocking

### 4.1 WireMock for HTTP APIs

**Toss Payments API Mock:**
```java
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(WireMockExtension.class)
class PaymentIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().port(8888))
        .build();

    @Test
    void confirmPayment_Success() throws Exception {
        // Given
        wireMock.stubFor(post("/v1/payments/confirm")
            .withHeader("Authorization", containing("Basic"))
            .withRequestBody(matchingJsonPath("$.paymentKey"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentKey": "test_payment_key",
                        "orderId": "order_123",
                        "status": "DONE",
                        "totalAmount": 10000
                    }
                    """)));

        // When
        PaymentConfirmRequest request = new PaymentConfirmRequest(
            "test_payment_key",
            "order_123",
            10000
        );

        PaymentResponse response = paymentService.confirmPayment(request);

        // Then
        assertThat(response.status()).isEqualTo("DONE");
        assertThat(response.totalAmount()).isEqualTo(10000);
    }

    @Test
    void confirmPayment_Failure_ShouldRetryAndFail() {
        // Given
        wireMock.stubFor(post("/v1/payments/confirm")
            .inScenario("Retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("Internal Server Error"))
            .willSetStateTo("First Retry"));

        wireMock.stubFor(post("/v1/payments/confirm")
            .inScenario("Retry")
            .whenScenarioStateIs("First Retry")
            .willReturn(aResponse()
                .withStatus(500))
            .willSetStateTo("Second Retry"));

        wireMock.stubFor(post("/v1/payments/confirm")
            .inScenario("Retry")
            .whenScenarioStateIs("Second Retry")
            .willReturn(aResponse()
                .withStatus(500)));

        // When / Then
        assertThatThrownBy(() -> paymentService.confirmPayment(request))
            .isInstanceOf(ExternalServiceException.class);

        // Verify 3 retry attempts
        wireMock.verify(3, postRequestedFor(urlEqualTo("/v1/payments/confirm")));
    }
}
```

### 4.2 Python VCR for API Recording

**Google AI API Mock:**
```python
# aiModel/tests/test_imagen_service.py
import pytest
import vcr
from src.services.imagen_service import GoogleAIImagenService

# Record real API responses once, then replay from cassette
@pytest.fixture(scope="module")
def vcr_cassette():
    return vcr.VCR(
        cassette_library_dir='tests/fixtures/vcr_cassettes',
        record_mode='once',  # 'once', 'new_episodes', 'none', 'all'
        match_on=['uri', 'method'],
    )

@vcr.use_cassette('tests/fixtures/vcr_cassettes/imagen_expand.yaml')
def test_expand_image_real_api(self):
    # This will use recorded response if cassette exists,
    # otherwise make real API call and record it
    service = GoogleAIImagenService(api_key=os.getenv("GOOGLE_AI_API_KEY"))

    with open("tests/fixtures/test_image.png", "rb") as f:
        result = service.expand_image(
            input_image=f.read(),
            expand_ratio=1.5,
            prompt="Expand clothing image with white background"
        )

    assert result is not None
    assert len(result) > 0
```

### 4.3 Testcontainers for Services

**MariaDB + RabbitMQ Integration:**
```java
@SpringBootTest
@Testcontainers
class FullStackIntegrationTest {

    @Container
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:10.11")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management")
        .withExposedPorts(5672, 15672);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Test
    void fullIntegration_WithRealDatabaseAndQueue() {
        // Test with real containers running
    }
}
```

## 5. End-to-End (E2E) Testing

### 5.1 Definition

**E2E tests** verify complete user journeys through the entire system, including frontend, backend, database, and external services (mocked).

**Characteristics:**
- Test from user perspective
- Cover critical business flows
- Use browser automation (Selenium, Playwright)
- Mock only truly external services
- Slowest tests (run less frequently)

### 5.2 Critical User Flows

**Priority 1 (Must Test):**
1. User signup → login → upload cloth → view processed result
2. User creates post → another user comments/likes → original user views notifications
3. User adds multiple clothes → views wardrobe → deletes item
4. Admin reviews reported post → takes moderation action

**Priority 2 (Should Test):**
5. User tries virtual try-on with uploaded clothes
6. User searches for posts by keyword
7. Payment flow (with mocked Toss API)

### 5.3 E2E Test Example (REST API)

**Using RestAssured:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
class UserJourneyE2ETest {

    @LocalServerPort
    private int port;

    private String baseUrl;
    private String authToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        baseUrl = "http://localhost:" + port;
    }

    @Test
    void userJourney_SignupLoginUploadCloth() {
        // 1. Signup
        SignupRequest signupRequest = new SignupRequest(
            "e2e@test.com",
            "Password123!",
            "E2EUser"
        );

        given()
            .contentType(ContentType.JSON)
            .body(signupRequest)
        .when()
            .post(baseUrl + "/api/v1/auth/signup")
        .then()
            .statusCode(201)
            .body("email", equalTo("e2e@test.com"));

        // 2. Login
        LoginRequest loginRequest = new LoginRequest(
            "e2e@test.com",
            "Password123!"
        );

        authToken = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("token");

        assertThat(authToken).isNotEmpty();

        // 3. Upload cloth image
        File testImage = new File("src/test/resources/test_cloth.jpg");

        Long clothId = given()
            .header("Authorization", "Bearer " + authToken)
            .multiPart("image", testImage, "image/jpeg")
            .formParam("category", "UPPER")
            .formParam("imageType", "SINGLE_ITEM")
        .when()
            .post(baseUrl + "/api/v1/cloth/upload")
        .then()
            .statusCode(200)
            .body("category", equalTo("UPPER"))
            .extract()
            .path("clothId");

        // 4. Poll for processing completion
        await().atMost(30, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                given()
                    .header("Authorization", "Bearer " + authToken)
                .when()
                    .get(baseUrl + "/api/v1/cloth/status/" + clothId)
                .then()
                    .statusCode(200)
                    .body("status", equalTo("COMPLETED"));
            });

        // 5. View wardrobe
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get(baseUrl + "/api/v1/cloth/my-closet")
        .then()
            .statusCode(200)
            .body("$.size()", greaterThan(0))
            .body("[0].clothId", equalTo(clothId));
    }
}
```

### 5.4 Frontend E2E Test (Playwright)

**User Flow Test:**
```typescript
// e2e/tests/cloth-upload.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Cloth Upload Flow', () => {
  test('user can signup, login, and upload cloth', async ({ page }) => {
    // 1. Navigate to signup
    await page.goto('http://localhost:3000/signup');

    // 2. Fill signup form
    await page.fill('input[name="email"]', 'playwright@test.com');
    await page.fill('input[name="password"]', 'Password123!');
    await page.fill('input[name="nickname"]', 'PlaywrightUser');
    await page.click('button[type="submit"]');

    // 3. Verify redirect to login
    await expect(page).toHaveURL(/.*login/);

    // 4. Login
    await page.fill('input[name="email"]', 'playwright@test.com');
    await page.fill('input[name="password"]', 'Password123!');
    await page.click('button[type="submit"]');

    // 5. Wait for redirect to dashboard
    await expect(page).toHaveURL(/.*dashboard/);

    // 6. Navigate to cloth upload
    await page.click('text=옷 추가하기');

    // 7. Select image type
    await page.check('input[value="SINGLE_ITEM"]');

    // 8. Upload image
    const fileInput = await page.locator('input[type="file"]');
    await fileInput.setInputFiles('tests/fixtures/test_cloth.jpg');

    // 9. Select category
    await page.selectOption('select[name="category"]', 'UPPER');

    // 10. Submit
    await page.click('button:has-text("업로드")');

    // 11. Wait for processing notification
    await expect(page.locator('.notification')).toContainText('처리 중');

    // 12. Wait for completion (polling)
    await expect(page.locator('.notification')).toContainText('완료', {
      timeout: 30000
    });

    // 13. Verify cloth appears in wardrobe
    await page.goto('http://localhost:3000/wardrobe');
    await expect(page.locator('.cloth-item')).toHaveCount(1);
  });
});
```

## 6. AI Model Testing

### 6.1 Model Accuracy Testing

**Segmentation Model Test:**
```python
# aiModel/tests/model/test_segmentation_accuracy.py
import pytest
import numpy as np
from PIL import Image
from src.utils.u2net_process import load_seg_model, generate_mask, get_palette

class TestSegmentationAccuracy:
    @pytest.fixture(scope="class")
    def model(self):
        return load_seg_model("model/cloth_segm.pth", device="cpu")

    @pytest.fixture
    def palette(self):
        return get_palette(4)

    def test_upper_cloth_detection(self, model, palette):
        """Test that model correctly segments upper clothing"""
        # Given: Known test image with upper clothing
        test_image = Image.open("tests/fixtures/labeled/upper_001.jpg")
        ground_truth_mask = Image.open("tests/fixtures/labeled/upper_001_mask.png")

        # When
        predicted_mask = generate_mask(test_image, model, palette)

        # Then: Calculate IoU (Intersection over Union)
        iou = calculate_iou(
            np.array(predicted_mask),
            np.array(ground_truth_mask)
        )

        assert iou > 0.85, f"IoU {iou} is below threshold 0.85"

    @pytest.mark.parametrize("category,min_iou", [
        ("upper", 0.85),
        ("lower", 0.80),
        ("dress", 0.82),
        ("shoes", 0.75),
    ])
    def test_category_accuracy(self, model, palette, category, min_iou):
        """Test accuracy across different clothing categories"""
        test_images = load_test_dataset(category)
        ious = []

        for image, ground_truth in test_images:
            predicted_mask = generate_mask(image, model, palette)
            iou = calculate_iou(
                np.array(predicted_mask),
                np.array(ground_truth)
            )
            ious.append(iou)

        mean_iou = np.mean(ious)
        assert mean_iou > min_iou, \
            f"{category} mean IoU {mean_iou:.3f} below threshold {min_iou}"

def calculate_iou(pred_mask, gt_mask):
    """Calculate Intersection over Union"""
    intersection = np.logical_and(pred_mask, gt_mask).sum()
    union = np.logical_or(pred_mask, gt_mask).sum()
    return intersection / union if union > 0 else 0
```

### 6.2 Model Performance Testing

**Response Time Test:**
```python
def test_segmentation_performance(model, palette):
    """Ensure segmentation completes within SLA"""
    test_image = Image.open("tests/fixtures/test_cloth.jpg")

    start = time.time()
    result = generate_mask(test_image, model, palette)
    duration = time.time() - start

    # SLA: Segmentation should complete in <5 seconds
    assert duration < 5.0, f"Segmentation took {duration:.2f}s (SLA: 5s)"

def test_batch_processing_throughput(model, palette):
    """Test throughput for batch processing"""
    test_images = [
        Image.open(f"tests/fixtures/batch/image_{i}.jpg")
        for i in range(10)
    ]

    start = time.time()
    results = [generate_mask(img, model, palette) for img in test_images]
    duration = time.time() - start

    throughput = len(test_images) / duration

    # SLA: Process at least 2 images per second
    assert throughput > 2.0, \
        f"Throughput {throughput:.2f} img/s below threshold 2.0 img/s"
```

### 6.3 Model Output Validation

**Sanity Checks:**
```python
def test_mask_output_validity(model, palette):
    """Ensure mask has valid properties"""
    test_image = Image.open("tests/fixtures/test_cloth.jpg")
    mask = generate_mask(test_image, model, palette)

    # Check mask dimensions match input
    assert mask.size == test_image.size

    # Check mask uses correct palette
    mask_array = np.array(mask)
    unique_values = np.unique(mask_array)
    assert all(v in [0, 1, 2, 3] for v in unique_values), \
        "Mask contains invalid class indices"

    # Check mask is not all background
    assert np.any(mask_array > 0), "Mask contains no foreground pixels"

    # Check mask coverage is reasonable (5-95% of image)
    foreground_ratio = np.sum(mask_array > 0) / mask_array.size
    assert 0.05 < foreground_ratio < 0.95, \
        f"Foreground ratio {foreground_ratio:.2%} outside reasonable range"
```

## 7. Performance & Load Testing

### 7.1 Load Test Scenarios

**Using Gatling (Scala):**
```scala
// src/test/scala/simulations/ClothUploadSimulation.scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class ClothUploadSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // Login and get JWT token
  val login = exec(http("Login")
    .post("/api/v1/auth/login")
    .body(StringBody("""{"email":"load@test.com","password":"Password123!"}"""))
    .check(jsonPath("$.token").saveAs("authToken")))

  // Upload cloth image
  val uploadCloth = exec(http("Upload Cloth")
    .post("/api/v1/cloth/upload")
    .header("Authorization", "Bearer ${authToken}")
    .formUpload("image", "test_cloth.jpg")
    .formParam("category", "UPPER")
    .formParam("imageType", "SINGLE_ITEM")
    .check(status.is(200))
    .check(jsonPath("$.clothId").saveAs("clothId")))

  // Poll for processing status
  val checkStatus = repeat(10, "statusCheck") {
    exec(http("Check Status")
      .get("/api/v1/cloth/status/${clothId}")
      .header("Authorization", "Bearer ${authToken}")
      .check(status.is(200)))
    .pause(2.seconds)
  }

  val scn = scenario("Cloth Upload Flow")
    .exec(login)
    .pause(1.second)
    .exec(uploadCloth)
    .exec(checkStatus)

  setUp(
    scn.inject(
      // Ramp up to 50 users over 2 minutes
      rampUsers(50).during(2.minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile(95).lt(2000), // P95 < 2s
     global.successfulRequests.percent.gt(95)     // 95% success rate
   )
}
```

### 7.2 Stress Testing

**Using Locust (Python):**
```python
# locustfile.py
from locust import HttpUser, task, between
import random

class ClothUploadUser(HttpUser):
    wait_time = between(1, 3)
    auth_token = None

    def on_start(self):
        """Login and get token"""
        response = self.client.post("/api/v1/auth/login", json={
            "email": f"user{random.randint(1, 1000)}@test.com",
            "password": "Password123!"
        })
        if response.status_code == 200:
            self.auth_token = response.json()["token"]

    @task(3)
    def view_wardrobe(self):
        """Frequently view wardrobe"""
        self.client.get(
            "/api/v1/cloth/my-closet",
            headers={"Authorization": f"Bearer {self.auth_token}"}
        )

    @task(2)
    def view_posts(self):
        """Browse community posts"""
        page = random.randint(0, 10)
        self.client.get(f"/api/v1/posts?page={page}&size=20")

    @task(1)
    def upload_cloth(self):
        """Occasionally upload new cloth"""
        with open("test_cloth.jpg", "rb") as image:
            self.client.post(
                "/api/v1/cloth/upload",
                headers={"Authorization": f"Bearer {self.auth_token}"},
                files={"image": image},
                data={"category": "UPPER", "imageType": "SINGLE_ITEM"}
            )
```

**Run stress test:**
```bash
# 1000 concurrent users, ramping up 100 users/second
locust -f locustfile.py --host=http://localhost:8080 \
  --users=1000 --spawn-rate=100 --run-time=10m
```

## 8. Security Testing

### 8.1 Authentication Tests

```java
@Test
void accessProtectedEndpoint_WithoutToken_ShouldReturn401() throws Exception {
    mvc.perform(get("/api/v1/cloth/my-closet"))
        .andExpect(status().isUnauthorized());
}

@Test
void accessProtectedEndpoint_WithExpiredToken_ShouldReturn401() throws Exception {
    String expiredToken = generateExpiredJwtToken();

    mvc.perform(get("/api/v1/cloth/my-closet")
            .header("Authorization", "Bearer " + expiredToken))
        .andExpect(status().isUnauthorized());
}

@Test
void accessProtectedEndpoint_WithInvalidSignature_ShouldReturn401() throws Exception {
    String tamperedToken = generateTamperedJwtToken();

    mvc.perform(get("/api/v1/cloth/my-closet")
            .header("Authorization", "Bearer " + tamperedToken))
        .andExpect(status().isUnauthorized());
}
```

### 8.2 Authorization Tests

```java
@Test
void deletePost_AsNonAuthor_ShouldReturn403() throws Exception {
    // User A creates post
    String tokenA = generateJwtToken(userA);
    Long postId = createPost(tokenA, "Test Post");

    // User B tries to delete
    String tokenB = generateJwtToken(userB);
    mvc.perform(delete("/api/v1/posts/" + postId)
            .header("Authorization", "Bearer " + tokenB))
        .andExpect(status().isForbidden());
}

@Test
void accessAdminEndpoint_AsRegularUser_ShouldReturn403() throws Exception {
    String userToken = generateJwtToken(regularUser);

    mvc.perform(delete("/api/v1/admin/posts/1")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden());
}
```

### 8.3 Input Validation Tests

```java
@Test
void uploadCloth_WithInvalidCategory_ShouldReturn400() throws Exception {
    mvc.perform(multipart("/api/v1/cloth/upload")
            .file("image", "test".getBytes())
            .param("category", "INVALID_CATEGORY"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_CATEGORY"));
}

@Test
void createPost_WithXSSPayload_ShouldSanitize() throws Exception {
    String xssPayload = "<script>alert('XSS')</script>";

    String response = mvc.perform(post("/api/v1/posts")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(String.format("""
                {
                    "title": "%s",
                    "content": "Safe content"
                }
                """, xssPayload)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    // Verify XSS is sanitized
    assertThat(response).doesNotContain("<script>");
}
```

## 9. CI Test Execution Strategy

### 9.1 Test Stages

```yaml
# .github/workflows/ci.yml
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - name: Run unit tests (fast)
        run: ./gradlew test --tests '*Test' -x integrationTest

  integration-tests:
    runs-on: ubuntu-latest
    needs: unit-tests
    steps:
      - name: Run integration tests
        run: ./gradlew integrationTest

  e2e-tests:
    runs-on: ubuntu-latest
    needs: integration-tests
    if: github.event_name == 'pull_request'
    steps:
      - name: Start services
        run: docker-compose up -d

      - name: Run E2E tests
        run: ./gradlew e2eTest

      - name: Teardown
        if: always()
        run: docker-compose down

  performance-tests:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - name: Run load tests
        run: |
          ./gradlew bootJar
          java -jar build/libs/*.jar &
          sleep 30
          locust -f locustfile.py --headless --users=100 --spawn-rate=10 --run-time=5m
```

### 9.2 Quality Gates

**PR Merge Requirements:**
- ✅ All unit tests pass (100%)
- ✅ All integration tests pass (100%)
- ✅ Code coverage ≥ 80% on new code
- ✅ No critical security vulnerabilities
- ✅ Static analysis passes (0 blocker issues)
- ⚠️ E2E tests pass (run on demand, not blocking)

**Main Branch Protection:**
- ✅ All above requirements
- ✅ E2E tests pass (100%)
- ✅ Performance tests pass (P95 < 2s)
- ✅ Manual approval from code owner

## 10. Test Data Management

### 10.1 Test Fixtures

**Shared Test Data:**
```java
// src/test/java/fixtures/TestDataFactory.java
public class TestDataFactory {
    public static Users createTestUser(String email) {
        return Users.builder()
            .email(email)
            .password("$2a$10$hashedPassword")  // BCrypt hash of "Password123!"
            .nickname("Test User")
            .role(UserRole.ROLE_USER)
            .status(UserStatus.NORMAL)
            .build();
    }

    public static Post createTestPost(Users author) {
        return Post.builder()
            .title("Test Post")
            .content("Test content")
            .author(author)
            .build();
    }

    public static Cloth createTestCloth(Users user) {
        return Cloth.builder()
            .user(user)
            .imagePath("/uploads/test.jpg")
            .category(ClothCategory.UPPER)
            .status(ProcessingStatus.COMPLETED)
            .build();
    }
}
```

### 10.2 Database Seeding

**Test Data SQL:**
```sql
-- src/test/resources/test-data.sql
INSERT INTO users (user_id, email, password, nickname, role, status)
VALUES
    (1, 'user1@test.com', '$2a$10$hash1', 'User 1', 'ROLE_USER', 'NORMAL'),
    (2, 'user2@test.com', '$2a$10$hash2', 'User 2', 'ROLE_USER', 'NORMAL'),
    (999, 'admin@test.com', '$2a$10$hashAdmin', 'Admin', 'ROLE_ADMIN', 'NORMAL');

INSERT INTO cloth (cloth_id, user_id, image_path, category, status)
VALUES
    (1, 1, '/uploads/test1.jpg', 'UPPER', 'COMPLETED'),
    (2, 1, '/uploads/test2.jpg', 'LOWER', 'COMPLETED'),
    (3, 2, '/uploads/test3.jpg', 'DRESS', 'COMPLETED');
```

### 10.3 Cleanup Strategy

```java
@AfterEach
void cleanup() {
    // Delete in reverse dependency order
    commentRepository.deleteAll();
    likeRepository.deleteAll();
    postRepository.deleteAll();
    clothRepository.deleteAll();
    userRepository.deleteAll();
}

// For faster cleanup in integration tests
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
```

---

**Document Version**: 1.0
**Last Updated**: 2025-12-31
**Next Review**: 2026-03-31
