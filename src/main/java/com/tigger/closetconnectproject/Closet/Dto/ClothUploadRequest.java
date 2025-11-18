package com.tigger.closetconnectproject.Closet.Dto;

import com.tigger.closetconnectproject.Closet.Entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 옷 이미지 업로드 요청 DTO
 * - multipart/form-data로 전송
 * - image 파일 + 옷 정보 (name, category)
 */
@Getter
@Setter
public class ClothUploadRequest {

    @NotBlank(message = "옷 이름은 필수입니다.")
    private String name;

    @NotNull(message = "카테고리는 필수입니다.")
    private Category category;
}
