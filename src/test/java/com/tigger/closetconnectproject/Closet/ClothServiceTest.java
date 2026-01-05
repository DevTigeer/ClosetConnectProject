package com.tigger.closetconnectproject.Closet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigger.closetconnectproject.Closet.Dto.ClothCreateRequest;
import com.tigger.closetconnectproject.Closet.Dto.ClothResponse;
import com.tigger.closetconnectproject.Closet.Entity.Category;
import com.tigger.closetconnectproject.Closet.Entity.Cloth;
import com.tigger.closetconnectproject.Closet.Entity.ProcessingStatus;
import com.tigger.closetconnectproject.Closet.Repository.ClothRepository;
import com.tigger.closetconnectproject.Closet.Service.ClothService;
import com.tigger.closetconnectproject.Closet.Service.ImageStorageService;
import com.tigger.closetconnectproject.User.Entity.UserRole;
import com.tigger.closetconnectproject.User.Entity.UserStatus;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * ClothService 단위 테스트
 * - 옷 생성, 조회, 삭제 테스트
 * - JSON 파싱 로직 테스트
 * - 권한 검증 테스트
 */
@ExtendWith(MockitoExtension.class)
class ClothServiceTest {

    @Mock
    private ClothRepository clothRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ClothService clothService;

    private Users testUser;
    private Cloth testCloth;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // ObjectMapper를 ClothService에 주입 (리플렉션 사용)
        try {
            var field = ClothService.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(clothService, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        testUser = Users.builder()
                .userId(1L)
                .email("test@test.com")
                .password("encoded")
                .nickname("테스터")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();

        testCloth = Cloth.builder()
                .id(100L)
                .user(testUser)
                .name("테스트 옷")
                .category(Category.TOP)
                .imageUrl("/uploads/original/100.png")
                .originalImageUrl("/uploads/original/100.png")
                .processingStatus(ProcessingStatus.COMPLETED)
                .confirmed(true)
                .build();
    }

    @Test
    @DisplayName("옷을 생성할 수 있다")
    void create() {
        // Given
        ClothCreateRequest req = new ClothCreateRequest(
                "새 옷",
                Category.BOTTOM,
                "/uploads/test.png"
        );

        given(usersRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(clothRepository.save(any(Cloth.class))).willReturn(testCloth);

        // When
        ClothResponse response = clothService.create(1L, req);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        verify(usersRepository).findById(1L);
        verify(clothRepository).save(any(Cloth.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 옷 생성 시 예외 발생")
    void createWithNonExistentUser() {
        // Given
        ClothCreateRequest req = new ClothCreateRequest(
                "새 옷",
                Category.TOP,
                "/uploads/test.png"
        );
        given(usersRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> clothService.create(999L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자 없음");
    }

    @Test
    @DisplayName("옷 목록을 카테고리별로 조회할 수 있다")
    void listByCategory() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cloth> page = new PageImpl<>(List.of(testCloth));

        given(clothRepository.findByUser_UserIdAndCategoryAndConfirmedTrue(
                eq(1L), eq(Category.TOP), eq(pageable)
        )).willReturn(page);

        // When
        Page<ClothResponse> result = clothService.list(1L, Category.TOP, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(100L);
        assertThat(result.getContent().get(0).category()).isEqualTo(Category.TOP);
    }

    @Test
    @DisplayName("전체 옷 목록을 조회할 수 있다")
    void listAll() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cloth> page = new PageImpl<>(List.of(testCloth));

        given(clothRepository.findByUser_UserIdAndConfirmedTrue(
                eq(1L), eq(pageable)
        )).willReturn(page);

        // When
        Page<ClothResponse> result = clothService.list(1L, null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("단일 옷 조회 시 본인 소유가 아니면 예외 발생")
    void getOneWithUnauthorizedUser() {
        // Given
        given(clothRepository.findById(100L)).willReturn(Optional.of(testCloth));

        // When & Then
        assertThatThrownBy(() -> clothService.getOne(999L, 100L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("본인 소유가 아닙니다");
    }

    @Test
    @DisplayName("옷을 삭제할 수 있다")
    void delete() {
        // Given
        given(clothRepository.findById(100L)).willReturn(Optional.of(testCloth));

        // When
        clothService.delete(1L, 100L);

        // Then
        verify(clothRepository).delete(testCloth);
    }

    @Test
    @DisplayName("옷 삭제 시 본인 소유가 아니면 예외 발생")
    void deleteWithUnauthorizedUser() {
        // Given
        given(clothRepository.findById(100L)).willReturn(Optional.of(testCloth));

        // When & Then
        assertThatThrownBy(() -> clothService.delete(999L, 100L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("본인 소유가 아닙니다");
    }

    @Test
    @DisplayName("단일 옷을 조회할 수 있다")
    void getOne() {
        // Given
        given(clothRepository.findById(100L)).willReturn(Optional.of(testCloth));

        // When
        ClothResponse response = clothService.getOne(1L, 100L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.name()).isEqualTo("테스트 옷");
        assertThat(response.category()).isEqualTo(Category.TOP);
    }

    @Test
    @DisplayName("존재하지 않는 옷 조회 시 예외 발생")
    void getOneWithNonExistentCloth() {
        // Given
        given(clothRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> clothService.getOne(1L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("아이템을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("처리 완료된 옷의 표시 이미지 URL은 imageUrl이다")
    void getDisplayImageUrl_completed() {
        // Given
        testCloth.setImageUrl("/uploads/final.png");
        testCloth.setInpaintedImageUrl("/uploads/inpainted.png");
        given(clothRepository.findById(100L)).willReturn(Optional.of(testCloth));

        // When
        ClothResponse response = clothService.getOne(1L, 100L);

        // Then
        assertThat(response.imageUrl()).isEqualTo("/uploads/final.png");
    }

    @Test
    @DisplayName("처리 중인 옷의 표시 이미지 URL은 원본 이미지이다")
    void getDisplayImageUrl_processing() {
        // Given
        testCloth.setProcessingStatus(ProcessingStatus.PROCESSING);
        testCloth.setImageUrl(null);
        testCloth.setInpaintedImageUrl(null);
        testCloth.setSegmentedImageUrl(null);
        given(clothRepository.findById(100L)).willReturn(Optional.of(testCloth));

        // When
        ClothResponse response = clothService.getOne(1L, 100L);

        // Then
        assertThat(response.imageUrl()).isEqualTo("/uploads/original/100.png");
    }

    @Test
    @DisplayName("AI 처리 완료 상태에서 imageUrl이 없으면 inpaintedImageUrl을 사용한다")
    void getDisplayImageUrl_readyForReview() {
        // Given
        testCloth.setProcessingStatus(ProcessingStatus.READY_FOR_REVIEW);
        testCloth.setImageUrl(null);
        testCloth.setInpaintedImageUrl("/uploads/inpainted.png");
        given(clothRepository.findById(100L)).willReturn(Optional.of(testCloth));

        // When
        ClothResponse response = clothService.getOne(1L, 100L);

        // Then
        assertThat(response.imageUrl()).isEqualTo("/uploads/inpainted.png");
    }

    @Test
    @DisplayName("JSON 파싱 실패 시 빈 리스트를 반환한다")
    void parseJsonToList_failureReturnsEmptyList() {
        // Given - JSON 파싱이 실패할 잘못된 JSON을 가진 옷
        Cloth clothWithInvalidJson = Cloth.builder()
                .id(101L)
                .user(testUser)
                .name("잘못된 JSON")
                .category(Category.TOP)
                .originalImageUrl("/uploads/original/101.png")
                .additionalItemsJson("invalid json {]")  // 잘못된 JSON
                .processingStatus(ProcessingStatus.COMPLETED)
                .confirmed(true)
                .build();

        given(clothRepository.findById(101L)).willReturn(Optional.of(clothWithInvalidJson));

        // When
        ClothResponse response = clothService.getOne(1L, 101L);

        // Then - 빈 리스트가 반환되어야 함
        assertThat(response).isNotNull();
        assertThat(response.additionalItems()).isEmpty();
    }

    @Test
    @DisplayName("null JSON은 null을 반환한다")
    void parseJsonToList_nullReturnsNull() {
        // Given
        Cloth clothWithNullJson = Cloth.builder()
                .id(102L)
                .user(testUser)
                .name("JSON 없음")
                .category(Category.TOP)
                .originalImageUrl("/uploads/original/102.png")
                .additionalItemsJson(null)
                .processingStatus(ProcessingStatus.COMPLETED)
                .confirmed(true)
                .build();

        given(clothRepository.findById(102L)).willReturn(Optional.of(clothWithNullJson));

        // When
        ClothResponse response = clothService.getOne(1L, 102L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.additionalItems()).isNull();
    }

    @Test
    @DisplayName("유효한 JSON은 정상적으로 파싱된다")
    void parseJsonToList_validJson() throws Exception {
        // Given
        String validJson = objectMapper.writeValueAsString(List.of(
                java.util.Map.of("label", "bag", "imageUrl", "/uploads/bag.png", "areaPixels", 1000),
                java.util.Map.of("label", "shoes", "imageUrl", "/uploads/shoes.png", "areaPixels", 800)
        ));

        Cloth clothWithValidJson = Cloth.builder()
                .id(103L)
                .user(testUser)
                .name("유효한 JSON")
                .category(Category.TOP)
                .originalImageUrl("/uploads/original/103.png")
                .additionalItemsJson(validJson)
                .processingStatus(ProcessingStatus.COMPLETED)
                .confirmed(true)
                .build();

        given(clothRepository.findById(103L)).willReturn(Optional.of(clothWithValidJson));

        // When
        ClothResponse response = clothService.getOne(1L, 103L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.additionalItems()).hasSize(2);
        assertThat(response.additionalItems().get(0).label()).isEqualTo("bag");
        assertThat(response.additionalItems().get(0).imageUrl()).isEqualTo("/uploads/bag.png");
        assertThat(response.additionalItems().get(0).areaPixels()).isEqualTo(1000);
        assertThat(response.additionalItems().get(1).label()).isEqualTo("shoes");
    }
}
