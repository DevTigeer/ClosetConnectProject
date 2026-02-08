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
    private final String backendBaseUrl;

    public GeminiTryonClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${tryon.api.url:http://localhost:5001}") String tryonApiUrl,
            @Value("${file.upload-dir:./uploads}") String uploadsDir,
            @Value("${app.backend.base-url:http://localhost:8080}") String backendBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.tryonApiUrl = tryonApiUrl;
        this.uploadsDir = uploadsDir;
        this.backendBaseUrl = backendBaseUrl;
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
     * 이미지를 Base64로 인코딩 (URL 또는 로컬 파일 경로 지원)
     *
     * @param imagePathOrUrl 이미지 URL 또는 파일 경로
     * @return Base64 인코딩된 이미지 문자열 (data:image/png;base64,...)
     */
    private String encodeImageToBase64(String imagePathOrUrl) throws IOException {
        byte[] imageBytes;
        String mimeType = "image/png";

        // 상대 경로(/uploads/...)를 절대 URL로 변환
        String resolvedUrl = imagePathOrUrl;
        if (imagePathOrUrl.startsWith("/uploads/") || imagePathOrUrl.startsWith("uploads/")) {
            String path = imagePathOrUrl.startsWith("/") ? imagePathOrUrl : "/" + imagePathOrUrl;
            resolvedUrl = backendBaseUrl + path;
            log.info("상대 경로를 URL로 변환: {} -> {}", imagePathOrUrl, resolvedUrl);
        }

        // HTTP/HTTPS URL인 경우 다운로드
        if (resolvedUrl.startsWith("http://") || resolvedUrl.startsWith("https://")) {
            log.info("URL에서 이미지 다운로드: {}", resolvedUrl);
            try {
                java.net.URL url = new java.net.URL(resolvedUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);

                if (connection.getResponseCode() != 200) {
                    throw new IOException("이미지 다운로드 실패: HTTP " + connection.getResponseCode());
                }

                // Content-Type에서 MIME 타입 추출
                String contentType = connection.getContentType();
                if (contentType != null && contentType.startsWith("image/")) {
                    mimeType = contentType.split(";")[0].trim();
                }

                try (java.io.InputStream is = connection.getInputStream();
                     java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    imageBytes = baos.toByteArray();
                }
                log.info("이미지 다운로드 완료: {} bytes", imageBytes.length);
            } catch (Exception e) {
                throw new IOException("URL에서 이미지를 다운로드할 수 없습니다: " + resolvedUrl, e);
            }
        } else {
            // 로컬 파일 경로 처리
            File imageFile = new File(resolvedUrl);

            if (!imageFile.exists()) {
                throw new IOException("이미지 파일을 찾을 수 없습니다: " + resolvedUrl);
            }

            imageBytes = Files.readAllBytes(imageFile.toPath());

            // MIME 타입 추론
            String probedMimeType = Files.probeContentType(imageFile.toPath());
            if (probedMimeType != null) {
                mimeType = probedMimeType;
            }
        }

        // Base64 인코딩
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
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
