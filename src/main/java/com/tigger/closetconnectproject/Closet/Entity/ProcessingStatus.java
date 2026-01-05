package com.tigger.closetconnectproject.Closet.Entity;

/**
 * 옷 이미지 처리 상태
 */
public enum ProcessingStatus {
    /**
     * 초기 업로드 중
     */
    UPLOADING,

    /**
     * 비동기 처리 중 (rembg → segmentation → inpainting)
     */
    PROCESSING,

    /**
     * 처리 완료, 사용자 확인 대기
     */
    READY_FOR_REVIEW,

    /**
     * 사용자 확인 완료
     */
    COMPLETED,

    /**
     * 처리 실패
     */
    FAILED
}
