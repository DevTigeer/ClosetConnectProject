package com.tigger.closetconnectproject.Closet.Client;

import com.tigger.closetconnectproject.Closet.Entity.Cloth;

import java.util.List;

/**
 * Virtual Try-On 클라이언트 인터페이스
 * 다양한 Try-On 엔진 (Gemini, ComfyUI 등)을 교체 가능하도록 추상화
 */
public interface TryonClient {

    /**
     * 의류 아이템들을 조합하여 가상 착용 이미지 생성
     *
     * @param upperClothes 상의 (nullable)
     * @param lowerClothes 하의 (nullable)
     * @param shoes 신발 (nullable)
     * @param accessories 악세서리 리스트 (nullable)
     * @param prompt 커스텀 프롬프트 (nullable)
     * @return 생성된 이미지의 Base64 데이터 URL
     */
    String generateTryon(
            Cloth upperClothes,
            Cloth lowerClothes,
            Cloth shoes,
            List<Cloth> accessories,
            String prompt
    );

    /**
     * Try-On 서비스 사용 가능 여부 확인
     *
     * @return true면 사용 가능
     */
    boolean isAvailable();

    /**
     * 사용 중인 엔진 이름 반환
     *
     * @return 엔진 이름 (예: "Gemini", "ComfyUI")
     */
    String getEngineName();
}
