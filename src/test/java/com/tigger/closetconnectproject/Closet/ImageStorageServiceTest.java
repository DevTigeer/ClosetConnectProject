package com.tigger.closetconnectproject.Closet;

import com.tigger.closetconnectproject.Closet.Service.ImageStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ImageStorageService 단위 테스트
 * - ImageType Enum 테스트
 * - 이미지 저장 및 삭제 테스트
 * - 파일 검증 테스트
 */
class ImageStorageServiceTest {

    private ImageStorageService imageStorageService;

    @TempDir
    Path tempDir;

    private String uploadBaseDir;
    private String uploadBaseUrl;

    @BeforeEach
    void setUp() {
        imageStorageService = new ImageStorageService();

        // 임시 디렉토리 설정
        uploadBaseDir = tempDir.toString();
        uploadBaseUrl = "/uploads";

        // ReflectionTestUtils를 사용하여 private 필드 주입
        ReflectionTestUtils.setField(imageStorageService, "uploadBaseDir", uploadBaseDir);
        ReflectionTestUtils.setField(imageStorageService, "uploadBaseUrl", uploadBaseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        // 임시 파일 정리는 @TempDir이 자동으로 처리
    }

    @Test
    @DisplayName("ImageType Enum은 모든 타입에 대한 디렉토리와 설명을 제공한다")
    void imageTypeEnum() {
        // When & Then
        assertThat(ImageStorageService.ImageType.ORIGINAL.getDirectory()).isEqualTo("original");
        assertThat(ImageStorageService.ImageType.ORIGINAL.getDescription()).isEqualTo("원본 이미지");

        assertThat(ImageStorageService.ImageType.REMOVED_BG.getDirectory()).isEqualTo("removed-bg");
        assertThat(ImageStorageService.ImageType.REMOVED_BG.getDescription()).isEqualTo("배경 제거 이미지");

        assertThat(ImageStorageService.ImageType.SEGMENTED.getDirectory()).isEqualTo("segmented");
        assertThat(ImageStorageService.ImageType.SEGMENTED.getDescription()).isEqualTo("세그멘테이션 이미지");

        assertThat(ImageStorageService.ImageType.INPAINTED.getDirectory()).isEqualTo("inpainted");
        assertThat(ImageStorageService.ImageType.INPAINTED.getDescription()).isEqualTo("인페인팅 이미지");

        assertThat(ImageStorageService.ImageType.ADDITIONAL.getDirectory()).isEqualTo("additional");
        assertThat(ImageStorageService.ImageType.ADDITIONAL.getDescription()).isEqualTo("추가 감지 아이템");

        assertThat(ImageStorageService.ImageType.EXPANDED.getDirectory()).isEqualTo("expanded");
        assertThat(ImageStorageService.ImageType.EXPANDED.getDescription()).isEqualTo("확장 이미지");

        assertThat(ImageStorageService.ImageType.TRYON.getDirectory()).isEqualTo("tryon");
        assertThat(ImageStorageService.ImageType.TRYON.getDescription()).isEqualTo("Try-On 결과");
    }

    @Test
    @DisplayName("원본 이미지를 저장할 수 있다")
    void saveOriginalImage() throws IOException {
        // Given
        byte[] imageBytes = new byte[]{1, 2, 3, 4, 5};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                imageBytes
        );
        Long clothId = 100L;

        // When
        String url = imageStorageService.saveOriginalImage(file, clothId);

        // Then
        assertThat(url).isEqualTo("/uploads/original/100.jpg");

        // 실제 파일이 저장되었는지 확인
        Path savedFile = Paths.get(uploadBaseDir, "original", "100.jpg");
        assertThat(Files.exists(savedFile)).isTrue();
        byte[] savedBytes = Files.readAllBytes(savedFile);
        assertThat(savedBytes).isEqualTo(imageBytes);
    }

    @Test
    @DisplayName("배경 제거 이미지를 저장할 수 있다")
    void saveRemovedBgImage() throws IOException {
        // Given
        byte[] imageBytes = new byte[]{10, 20, 30};
        Long clothId = 200L;

        // When
        String url = imageStorageService.saveRemovedBgImage(imageBytes, clothId);

        // Then
        assertThat(url).isEqualTo("/uploads/removed-bg/200.png");

        // 실제 파일이 저장되었는지 확인
        Path savedFile = Paths.get(uploadBaseDir, "removed-bg", "200.png");
        assertThat(Files.exists(savedFile)).isTrue();
        byte[] savedBytes = Files.readAllBytes(savedFile);
        assertThat(savedBytes).isEqualTo(imageBytes);
    }

    @Test
    @DisplayName("세그멘테이션 이미지를 저장할 수 있다")
    void saveSegmentedImage() throws IOException {
        // Given
        byte[] imageBytes = new byte[]{5, 6, 7, 8};
        Long clothId = 300L;

        // When
        String url = imageStorageService.saveSegmentedImage(imageBytes, clothId);

        // Then
        assertThat(url).isEqualTo("/uploads/segmented/300.png");

        // 실제 파일이 저장되었는지 확인
        Path savedFile = Paths.get(uploadBaseDir, "segmented", "300.png");
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("인페인팅 이미지를 저장할 수 있다")
    void saveInpaintedImage() throws IOException {
        // Given
        byte[] imageBytes = new byte[]{11, 12, 13};
        Long clothId = 400L;

        // When
        String url = imageStorageService.saveInpaintedImage(imageBytes, clothId);

        // Then
        assertThat(url).isEqualTo("/uploads/inpainted/400.png");

        Path savedFile = Paths.get(uploadBaseDir, "inpainted", "400.png");
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("추가 아이템 이미지를 라벨과 함께 저장할 수 있다")
    void saveAdditionalItemImage() throws IOException {
        // Given
        byte[] imageBytes = new byte[]{15, 16, 17};
        Long clothId = 500L;
        String label = "bag";

        // When
        String url = imageStorageService.saveAdditionalItemImage(imageBytes, clothId, label);

        // Then
        assertThat(url).isEqualTo("/uploads/additional/500_bag.png");

        Path savedFile = Paths.get(uploadBaseDir, "additional", "500_bag.png");
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("세그먼트된 이미지를 라벨과 함께 저장할 수 있다")
    void saveSegmentedImageWithLabel() throws IOException {
        // Given
        byte[] imageBytes = new byte[]{20, 21, 22};
        Long clothId = 600L;
        String label = "upper-clothes";

        // When
        String url = imageStorageService.saveSegmentedImage(imageBytes, clothId, label);

        // Then
        assertThat(url).isEqualTo("/uploads/segmented/600_upper-clothes.png");

        Path savedFile = Paths.get(uploadBaseDir, "segmented", "600_upper-clothes.png");
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("확장된 이미지를 라벨과 함께 저장할 수 있다")
    void saveExpandedImage() throws IOException {
        // Given
        byte[] imageBytes = new byte[]{25, 26, 27};
        Long clothId = 700L;
        String label = "pants";

        // When
        String url = imageStorageService.saveExpandedImage(imageBytes, clothId, label);

        // Then
        assertThat(url).isEqualTo("/uploads/expanded/700_pants.png");

        Path savedFile = Paths.get(uploadBaseDir, "expanded", "700_pants.png");
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("Try-On 이미지를 저장할 수 있다")
    void saveImageBytes() throws IOException {
        // Given
        byte[] imageBytes = new byte[]{30, 31, 32};
        String filename = "tryon_result.png";

        // When
        String url = imageStorageService.saveImageBytes(imageBytes, filename);

        // Then
        assertThat(url).isEqualTo("/uploads/tryon/tryon_result.png");

        Path savedFile = Paths.get(uploadBaseDir, "tryon", "tryon_result.png");
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("이미지를 삭제할 수 있다")
    void deleteImage() throws IOException {
        // Given - 먼저 이미지 저장
        byte[] imageBytes = new byte[]{1, 2, 3};
        String url = imageStorageService.saveRemovedBgImage(imageBytes, 999L);
        Path savedFile = Paths.get(uploadBaseDir, "removed-bg", "999.png");
        assertThat(Files.exists(savedFile)).isTrue();

        // When
        imageStorageService.deleteImage(url);

        // Then
        assertThat(Files.exists(savedFile)).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 이미지 삭제는 예외를 발생시키지 않는다")
    void deleteNonExistentImage() {
        // When & Then - 예외가 발생하지 않아야 함
        imageStorageService.deleteImage("/uploads/original/non-existent.png");
    }

    @Test
    @DisplayName("null 이미지 URL 삭제는 예외를 발생시키지 않는다")
    void deleteNullImageUrl() {
        // When & Then
        imageStorageService.deleteImage(null);
    }

    @Test
    @DisplayName("빈 이미지 URL 삭제는 예외를 발생시키지 않는다")
    void deleteEmptyImageUrl() {
        // When & Then
        imageStorageService.deleteImage("");
        imageStorageService.deleteImage("  ");
    }

    @Test
    @DisplayName("빈 파일 업로드 시 예외 발생")
    void validateEmptyFile() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        // When & Then
        assertThatThrownBy(() -> imageStorageService.saveOriginalImage(emptyFile, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("비어있습니다")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("10MB를 초과하는 파일 업로드 시 예외 발생")
    void validateOversizeFile() {
        // Given - 10MB + 1 byte
        byte[] oversizeBytes = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile oversizeFile = new MockMultipartFile(
                "file",
                "oversize.jpg",
                "image/jpeg",
                oversizeBytes
        );

        // When & Then
        assertThatThrownBy(() -> imageStorageService.saveOriginalImage(oversizeFile, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("10MB")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("이미지가 아닌 파일 업로드 시 예외 발생")
    void validateNonImageFile() {
        // Given
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                new byte[]{1, 2, 3}
        );

        // When & Then
        assertThatThrownBy(() -> imageStorageService.saveOriginalImage(textFile, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미지 파일만")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Content-Type이 null인 파일 업로드 시 예외 발생")
    void validateNullContentType() {
        // Given
        MockMultipartFile fileWithNullContentType = new MockMultipartFile(
                "file",
                "unknown.dat",
                null,
                new byte[]{1, 2, 3}
        );

        // When & Then
        assertThatThrownBy(() -> imageStorageService.saveOriginalImage(fileWithNullContentType, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미지 파일만")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("PNG 이미지 파일을 저장할 수 있다")
    void savePngImage() throws IOException {
        // Given
        byte[] imageBytes = new byte[]{1, 2, 3};
        MockMultipartFile pngFile = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                imageBytes
        );

        // When
        String url = imageStorageService.saveOriginalImage(pngFile, 101L);

        // Then
        assertThat(url).isEqualTo("/uploads/original/101.png");
        Path savedFile = Paths.get(uploadBaseDir, "original", "101.png");
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("확장자가 없는 파일은 기본 .jpg 확장자로 저장된다")
    void saveFileWithoutExtension() throws IOException {
        // Given
        byte[] imageBytes = new byte[]{1, 2, 3};
        MockMultipartFile fileWithoutExt = new MockMultipartFile(
                "file",
                "noextension",
                "image/jpeg",
                imageBytes
        );

        // When
        String url = imageStorageService.saveOriginalImage(fileWithoutExt, 102L);

        // Then
        assertThat(url).isEqualTo("/uploads/original/102.jpg");
    }

    @Test
    @DisplayName("파일명이 null이면 기본 .jpg 확장자로 저장된다")
    void saveFileWithNullFilename() throws IOException {
        // Given
        byte[] imageBytes = new byte[]{1, 2, 3};
        MockMultipartFile fileWithNullName = new MockMultipartFile(
                "file",
                null,
                "image/jpeg",
                imageBytes
        );

        // When
        String url = imageStorageService.saveOriginalImage(fileWithNullName, 103L);

        // Then
        assertThat(url).isEqualTo("/uploads/original/103.jpg");
    }

    @Test
    @DisplayName("동일한 파일명으로 저장 시 기존 파일이 덮어써진다")
    void overwriteExistingFile() throws IOException {
        // Given
        byte[] originalBytes = new byte[]{1, 2, 3};
        byte[] newBytes = new byte[]{4, 5, 6, 7};

        imageStorageService.saveRemovedBgImage(originalBytes, 1000L);

        // When
        imageStorageService.saveRemovedBgImage(newBytes, 1000L);

        // Then
        Path savedFile = Paths.get(uploadBaseDir, "removed-bg", "1000.png");
        byte[] savedBytes = Files.readAllBytes(savedFile);
        assertThat(savedBytes).isEqualTo(newBytes);
    }
}
