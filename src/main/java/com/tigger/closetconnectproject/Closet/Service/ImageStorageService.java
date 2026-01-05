package com.tigger.closetconnectproject.Closet.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 이미지 파일 저장 서비스
 * - 원본 이미지와 AI 처리된 이미지를 로컬 파일 시스템에 저장
 * - 저장 경로: /uploads/{imageType}/{filename}
 */
@Slf4j
@Service
public class ImageStorageService {

    @Value("${upload.dir:./uploads}")
    private String uploadBaseDir;

    @Value("${upload.base-url:/uploads}")
    private String uploadBaseUrl;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 이미지 타입별 저장 디렉토리와 설명을 정의하는 Enum
     */
    public enum ImageType {
        ORIGINAL("original", "원본 이미지"),
        REMOVED_BG("removed-bg", "배경 제거 이미지"),
        SEGMENTED("segmented", "세그멘테이션 이미지"),
        INPAINTED("inpainted", "인페인팅 이미지"),
        ADDITIONAL("additional", "추가 감지 아이템"),
        EXPANDED("expanded", "확장 이미지"),
        TRYON("tryon", "Try-On 결과");

        private final String directory;
        private final String description;

        ImageType(String directory, String description) {
            this.directory = directory;
            this.description = description;
        }

        public String getDirectory() {
            return directory;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 이미지를 저장하는 범용 메서드
     *
     * @param imageBytes 이미지 바이트 배열
     * @param imageType  이미지 타입 (저장 디렉토리 결정)
     * @param filename   파일명
     * @return 저장된 이미지의 URL
     */
    private String saveImage(byte[] imageBytes, ImageType imageType, String filename) {
        try {
            // 저장 디렉토리 생성
            Path uploadPath = Paths.get(uploadBaseDir, imageType.getDirectory());
            Files.createDirectories(uploadPath);

            // 파일 저장
            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, imageBytes);

            // URL 생성 및 반환
            String url = uploadBaseUrl + "/" + imageType.getDirectory() + "/" + filename;
            log.info("Saved {} image: {}", imageType.getDescription(), url);

            return url;

        } catch (IOException e) {
            log.error("Failed to save {} image: {}", imageType.getDescription(), filename, e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    imageType.getDescription() + " 저장 실패: " + e.getMessage()
            );
        }
    }

    /**
     * 원본 이미지 저장
     *
     * @param file 업로드된 이미지 파일
     * @param clothId 옷 ID
     * @return 저장된 이미지의 URL
     */
    public String saveOriginalImage(MultipartFile file, Long clothId) {
        validateImage(file);

        try {
            // 저장 디렉토리 생성
            Path uploadPath = Paths.get(uploadBaseDir, ImageType.ORIGINAL.getDirectory());
            Files.createDirectories(uploadPath);

            // 파일 확장자 추출
            String originalFilename = file.getOriginalFilename();
            String extension = getExtension(originalFilename);

            // 파일명 생성: {clothId}.{ext}
            String filename = clothId + extension;
            Path filePath = uploadPath.resolve(filename);

            // 파일 저장
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // URL 반환
            String url = uploadBaseUrl + "/" + ImageType.ORIGINAL.getDirectory() + "/" + filename;
            log.info("Saved {} image: {}", ImageType.ORIGINAL.getDescription(), url);

            return url;

        } catch (IOException e) {
            log.error("Failed to save {} image for clothId: {}", ImageType.ORIGINAL.getDescription(), clothId, e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ImageType.ORIGINAL.getDescription() + " 저장 실패: " + e.getMessage()
            );
        }
    }

    /**
     * 배경 제거 이미지 저장
     *
     * @param imageBytes 배경이 제거된 PNG 이미지 바이트 배열
     * @param clothId 옷 ID
     * @return 저장된 이미지의 URL
     */
    public String saveRemovedBgImage(byte[] imageBytes, Long clothId) {
        String filename = clothId + ".png";
        return saveImage(imageBytes, ImageType.REMOVED_BG, filename);
    }

    /**
     * 세그멘테이션 결과 이미지 저장 (크롭된 옷 이미지)
     *
     * @param imageBytes 크롭된 옷 이미지 바이트 배열
     * @param clothId 옷 ID
     * @return 저장된 이미지의 URL
     */
    public String saveSegmentedImage(byte[] imageBytes, Long clothId) {
        String filename = clothId + ".png";
        return saveImage(imageBytes, ImageType.SEGMENTED, filename);
    }

    /**
     * Inpainting 결과 이미지 저장 (복원된 최종 이미지)
     *
     * @param imageBytes 복원된 이미지 바이트 배열
     * @param clothId 옷 ID
     * @return 저장된 이미지의 URL
     */
    public String saveInpaintedImage(byte[] imageBytes, Long clothId) {
        String filename = clothId + ".png";
        return saveImage(imageBytes, ImageType.INPAINTED, filename);
    }

    /**
     * 추가 감지된 아이템 이미지 저장
     *
     * @param imageBytes 이미지 바이트 배열
     * @param clothId 옷 ID
     * @param label 아이템 라벨 (예: "upper-clothes", "pants" 등)
     * @return 저장된 이미지의 URL
     */
    public String saveAdditionalItemImage(byte[] imageBytes, Long clothId, String label) {
        String filename = clothId + "_" + label + ".png";
        return saveImage(imageBytes, ImageType.ADDITIONAL, filename);
    }

    /**
     * 세그먼트된 이미지 저장 (라벨 포함, 여러 아이템용)
     * @param imageBytes 이미지 바이트 데이터
     * @param clothId 옷 ID
     * @param label 아이템 라벨 (예: "upper-clothes", "pants" 등)
     * @return 저장된 이미지의 URL
     */
    public String saveSegmentedImage(byte[] imageBytes, Long clothId, String label) {
        String filename = clothId + "_" + label + ".png";
        return saveImage(imageBytes, ImageType.SEGMENTED, filename);
    }

    /**
     * Gemini 확장된 이미지 저장
     * @param imageBytes 이미지 바이트 데이터
     * @param clothId 옷 ID
     * @param label 아이템 라벨 (예: "upper-clothes", "pants" 등)
     * @return 저장된 이미지의 URL
     */
    public String saveExpandedImage(byte[] imageBytes, Long clothId, String label) {
        String filename = clothId + "_" + label + ".png";
        return saveImage(imageBytes, ImageType.EXPANDED, filename);
    }

    /**
     * 이미지 파일 삭제
     *
     * @param imageUrl 삭제할 이미지 URL
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            // URL에서 파일 경로 추출
            // 예: /uploads/original/123.jpg -> ./uploads/original/123.jpg
            String relativePath = imageUrl.replace(uploadBaseUrl, uploadBaseDir);
            Path filePath = Paths.get(relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted image: {}", imageUrl);
            }

        } catch (IOException e) {
            log.warn("Failed to delete image: {}", imageUrl, e);
            // 삭제 실패는 크리티컬하지 않으므로 예외를 던지지 않음
        }
    }

    /**
     * 이미지 파일 검증
     *
     * @param file 업로드된 파일
     * @throws ResponseStatusException 검증 실패 시
     */
    private void validateImage(MultipartFile file) {
        // 파일이 비어있는지 확인
        if (file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "업로드된 파일이 비어있습니다."
            );
        }

        // 파일 크기 확인
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "파일 크기는 10MB를 초과할 수 없습니다."
            );
        }

        // 파일 형식 확인 (이미지만 허용)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미지 파일만 업로드 가능합니다."
            );
        }
    }

    /**
     * Try-On 결과 이미지 저장 (범용 바이트 배열 저장)
     *
     * @param imageBytes 이미지 바이트 배열
     * @param filename 파일명 (확장자 포함)
     * @return 저장된 이미지의 URL
     */
    public String saveImageBytes(byte[] imageBytes, String filename) {
        return saveImage(imageBytes, ImageType.TRYON, filename);
    }

    /**
     * 파일 확장자 추출
     *
     * @param filename 파일명
     * @return 확장자 (.jpg, .png 등)
     */
    private String getExtension(String filename) {
        if (filename == null) {
            return ".jpg";
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex);
        }

        return ".jpg";
    }
}
