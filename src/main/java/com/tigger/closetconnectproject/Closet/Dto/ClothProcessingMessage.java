package com.tigger.closetconnectproject.Closet.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RabbitMQ 메시지 DTO
 * - 옷 이미지 처리 파이프라인을 위한 메시지 페이로드
 * - Serializable: RabbitMQ 메시지로 직렬화 가능
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClothProcessingMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 옷 아이템 ID (데이터베이스 PK)
     */
    private Long clothId;

    /**
     * 사용자 ID (WebSocket 알림용)
     */
    private Long userId;

    /**
     * 원본 이미지 바이트 배열
     * - Base64로 인코딩하여 전송 (JSON 직렬화)
     */
    private byte[] imageBytes;

    /**
     * 원본 파일명
     * - 확장자 추출에 사용
     */
    private String originalFilename;

    /**
     * 이미지 타입
     * - FULL_BODY: 전신 사진 (Segformer)
     * - SINGLE_ITEM: 단일 옷 이미지 (U2NET)
     */
    private String imageType;

    /**
     * 재시도 횟수 (선택 사항)
     */
    private int retryCount = 0;

    /**
     * 메시지 생성 시간 (밀리초, Unix timestamp)
     * - 오래된 메시지 폐기용
     */
    private long timestamp = System.currentTimeMillis();
}
