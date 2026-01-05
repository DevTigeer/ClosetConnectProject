package com.tigger.closetconnectproject.Closet.Client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigger.closetconnectproject.Closet.Entity.Cloth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Gemini 기반 Virtual Try-On 클라이언트
 * Python API 서버를 호출하여 try-on 이미지 생성
 */
@Slf4j
@Component
public class GeminiTryonClient implements TryonClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String tryonApiUrl;
    private final String uploadsDir;

    public GeminiTryonClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${tryon.api.url:http://localhost:5001}") String tryonApiUrl,
            @Value("${file.upload-dir:./uploads}") String uploadsDir
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.tryonApiUrl = tryonApiUrl;
        this.uploadsDir = uploadsDir;
    }

    @Override
    public String generateTryon(
            Cloth upperClothes,
            Cloth lowerClothes,
            Cloth shoes,
            List<Cloth> accessories,
            String prompt
    ) {
        try {
            log.info("Gemini Try-On 생성 시작");

            // 요청 바디 구성
            Map<String, Object> requestBody = new HashMap<>();

            // 상의
            if (upperClothes != null) {
                String imageUrl = getBestImageUrl(upperClothes);
                if (imageUrl != null) {
                    String base64 = encodeImageToBase64(imageUrl);
                    requestBody.put("upperClothes", base64);
                    log.info("상의 추가: {}", imageUrl);
                }
            }

            // 하의
            if (lowerClothes != null) {
                String imageUrl = getBestImageUrl(lowerClothes);
                if (imageUrl != null) {
                    String base64 = encodeImageToBase64(imageUrl);
                    requestBody.put("lowerClothes", base64);
                    log.info("하의 추가: {}", imageUrl);
                }
            }

            // 신발
            if (shoes != null) {
                String imageUrl = getBestImageUrl(shoes);
                if (imageUrl != null) {
                    String base64 = encodeImageToBase64(imageUrl);
                    requestBody.put("shoes", base64);
                    log.info("신발 추가: {}", imageUrl);
                }
            }

            // 악세서리
            if (accessories != null && !accessories.isEmpty()) {
                List<String> accessoryBase64List = new ArrayList<>();
                for (Cloth accessory : accessories) {
                    String imageUrl = getBestImageUrl(accessory);
                    if (imageUrl != null) {
                        String base64 = encodeImageToBase64(imageUrl);
                        accessoryBase64List.add(base64);
                    }
                }
                requestBody.put("accessories", accessoryBase64List);
                log.info("악세서리 {}개 추가", accessoryBase64List.size());
            }

            // 프롬프트
            if (prompt != null && !prompt.isBlank()) {
                requestBody.put("prompt", prompt);
            }

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // HTTP 요청 생성
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Python API 호출
            String apiUrl = tryonApiUrl + "/tryon";
            log.info("Python API 호출: {}", apiUrl);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                // 응답 파싱
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                boolean success = jsonNode.get("success").asBoolean();

                if (success) {
                    String imageData = jsonNode.get("image").asText();
                    String engine = jsonNode.get("engine").asText();
                    log.info("Try-On 생성 성공 (엔진: {})", engine);
                    return imageData;
                } else {
                    String error = jsonNode.has("error") ? jsonNode.get("error").asText() : "Unknown error";
                    log.error("Try-On 생성 실패: {}", error);
                    throw new RuntimeException("Try-On 생성 실패: " + error);
                }
            } else {
                log.error("API 호출 실패: {}", response.getStatusCode());
                throw new RuntimeException("Try-On API 호출 실패: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Try-On 생성 중 오류 발생", e);
            throw new RuntimeException("Try-On 생성 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // Health check 엔드포인트 호출
            String healthUrl = tryonApiUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("tryon_available").asBoolean();
            }
            return false;
        } catch (Exception e) {
            log.warn("Try-On 서비스 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getEngineName() {
        return "Gemini";
    }

    /**
     * 이미지 파일을 Base64로 인코딩
     *
     * @param imagePath 이미지 파일 경로 (상대 경로 또는 절대 경로)
     * @return Base64 인코딩된 이미지 문자열 (data:image/png;base64,...)
     */
    private String encodeImageToBase64(String imagePath) throws IOException {
        // 파일 경로 처리
        File imageFile;

        // uploads로 시작하는 상대 경로인 경우
        if (imagePath.startsWith("uploads/") || imagePath.startsWith("/uploads/")) {
            String relativePath = imagePath.replaceFirst("^/uploads/", "uploads/");
            imageFile = new File(uploadsDir, relativePath.replace("uploads/", ""));
        } else {
            imageFile = new File(imagePath);
        }

        if (!imageFile.exists()) {
            throw new IOException("이미지 파일을 찾을 수 없습니다: " + imagePath);
        }

        // 파일을 바이트 배열로 읽기
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());

        // Base64 인코딩
        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        // MIME 타입 추론
        String mimeType = Files.probeContentType(imageFile.toPath());
        if (mimeType == null) {
            mimeType = "image/png"; // 기본값
        }

        return "data:" + mimeType + ";base64," + base64;
    }

    /**
     * Cloth 엔티티에서 가장 적합한 이미지 URL 반환
     * 우선순위: inpaintedImageUrl > segmentedImageUrl > removedBgImageUrl > imageUrl
     *
     * @param cloth 의류 엔티티
     * @return 이미지 URL (없으면 null)
     */
    private String getBestImageUrl(Cloth cloth) {
        if (cloth.getInpaintedImageUrl() != null && !cloth.getInpaintedImageUrl().isBlank()) {
            return cloth.getInpaintedImageUrl();
        }
        if (cloth.getSegmentedImageUrl() != null && !cloth.getSegmentedImageUrl().isBlank()) {
            return cloth.getSegmentedImageUrl();
        }
        if (cloth.getRemovedBgImageUrl() != null && !cloth.getRemovedBgImageUrl().isBlank()) {
            return cloth.getRemovedBgImageUrl();
        }
        if (cloth.getImageUrl() != null && !cloth.getImageUrl().isBlank()) {
            return cloth.getImageUrl();
        }
        return null;
    }
}
