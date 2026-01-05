package com.tigger.closetconnectproject.Closet.Event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Cloth 업로드 완료 이벤트
 * - 트랜잭션 커밋 후 비동기 처리를 시작하기 위한 이벤트
 */
@Getter
public class ClothUploadedEvent extends ApplicationEvent {
    private final Long clothId;
    private final Long userId;
    private final byte[] imageBytes;
    private final String originalFilename;
    private final String imageType;

    public ClothUploadedEvent(Object source, Long clothId, Long userId, byte[] imageBytes, String originalFilename, String imageType) {
        super(source);
        this.clothId = clothId;
        this.userId = userId;
        this.imageBytes = imageBytes;
        this.originalFilename = originalFilename;
        this.imageType = imageType;
    }
}
