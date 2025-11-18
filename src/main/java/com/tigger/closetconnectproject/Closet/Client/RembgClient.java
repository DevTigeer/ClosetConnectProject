package com.tigger.closetconnectproject.Closet.Client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;

/**
 * Python FastAPI rembg 서버 연동 클라이언트
 * - POST /remove-bg 엔드포인트 호출
 * - multipart/form-data로 이미지 전송
 * - 배경 제거된 PNG 이미지(byte[]) 수신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RembgClient {

    @Value("${rembg.server.url:http://localhost:8001}")
    private String rembgServerUrl;

    @Value("${rembg.timeout.seconds:10}")
    private int timeoutSeconds;

    private final WebClient.Builder webClientBuilder;

    /**
     * 배경 제거 요청
     *
     * @param imageFile 원본 이미지 파일
     * @return 배경이 제거된 PNG 이미지 (byte[])
     * @throws ResponseStatusException rembg 서버 호출 실패 시 502 Bad Gateway
     */
    public byte[] removeBackground(MultipartFile imageFile) {
        log.info("Calling rembg server to remove background: {}", imageFile.getOriginalFilename());

        try {
            // 파일 확장자 추출
            String originalFilename = imageFile.getOriginalFilename();
            String extension = getFileExtension(originalFilename);

            // ASCII 안전한 파일명으로 변경 (한글 파일명 인코딩 문제 방지)
            String safeFilename = "image" + extension;

            // MultipartBodyBuilder를 사용하여 multipart/form-data 구성
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", imageFile.getResource())
                    .filename(safeFilename);

            // WebClient로 rembg 서버 호출
            Flux<DataBuffer> dataBufferFlux = webClientBuilder
                    .baseUrl(rembgServerUrl)
                    .build()
                    .post()
                    .uri("/remove-bg")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .accept(MediaType.IMAGE_PNG)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            response -> {
                                log.error("rembg server returned error: {}", response.statusCode());
                                return response.bodyToMono(String.class)
                                        .map(body -> new ResponseStatusException(
                                                HttpStatus.BAD_GATEWAY,
                                                "rembg 서버 호출 실패: " + body
                                        ));
                            }
                    )
                    .bodyToFlux(DataBuffer.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds));

            // Flux<DataBuffer>를 byte[]로 변환
            byte[] result = dataBufferFlux
                    .collectList()
                    .map(dataBuffers -> {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        dataBuffers.forEach(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);
                            try {
                                outputStream.write(bytes);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        return outputStream.toByteArray();
                    })
                    .block();
            log.info("Successfully received background-removed image: {} bytes", result.length);

            return result;

        } catch (Exception e) {
            log.error("Failed to call rembg server", e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "rembg 서버 통신 실패: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 배경 제거 요청 (byte[] 입력)
     *
     * @param imageBytes 원본 이미지 바이트 배열
     * @param filename 파일명
     * @return 배경이 제거된 PNG 이미지 (byte[])
     */
    public byte[] removeBackground(byte[] imageBytes, String filename) {
        log.info("Calling rembg server to remove background: {} bytes", imageBytes.length);

        try {
            // 파일 확장자 추출
            String extension = getFileExtension(filename);
            // ASCII 안전한 파일명으로 변경
            String safeFilename = "image" + extension;

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", imageBytes)
                    .filename(safeFilename)
                    .contentType(MediaType.IMAGE_JPEG); // 또는 적절한 타입

            Flux<DataBuffer> dataBufferFlux = webClientBuilder
                    .baseUrl(rembgServerUrl)
                    .build()
                    .post()
                    .uri("/remove-bg")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .accept(MediaType.IMAGE_PNG)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            response -> {
                                log.error("rembg server returned error: {}", response.statusCode());
                                return response.bodyToMono(String.class)
                                        .map(body -> new ResponseStatusException(
                                                HttpStatus.BAD_GATEWAY,
                                                "rembg 서버 호출 실패: " + body
                                        ));
                            }
                    )
                    .bodyToFlux(DataBuffer.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds));

            byte[] result = dataBufferFlux
                    .collectList()
                    .map(dataBuffers -> {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        dataBuffers.forEach(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);
                            try {
                                outputStream.write(bytes);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        return outputStream.toByteArray();
                    })
                    .block();
            log.info("Successfully received background-removed image: {} bytes", result.length);

            return result;

        } catch (Exception e) {
            log.error("Failed to call rembg server", e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "rembg 서버 통신 실패: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 파일 확장자 추출 헬퍼 메서드
     *
     * @param filename 파일명
     * @return 확장자 (예: .jpg, .png)
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return ".png";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex).toLowerCase();
        }

        return ".png"; // 기본값
    }
}
