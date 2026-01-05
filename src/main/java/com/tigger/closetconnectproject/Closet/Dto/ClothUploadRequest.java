package com.tigger.closetconnectproject.Closet.Dto;

import com.tigger.closetconnectproject.Closet.Entity.Category;
import com.tigger.closetconnectproject.Closet.Entity.ImageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 옷 이미지 업로드 요청 DTO
 * - multipart/form-data로 전송
 * - image 파일 + 옷 정보 (name, category, imageType)
 */
@Getter
@Setter
public class ClothUploadRequest {

    @NotBlank(message = "옷 이름은 필수입니다.")
    private String name;

    private Category category;  // 선택적 (AI가 제안)

    private ImageType imageType;  // 선택적 (기본값: FULL_BODY)
}
