package com.tigger.closetconnectproject.Closet.Dto;

import com.tigger.closetconnectproject.Closet.Entity.Ootd;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class OotdDtos {

    public record CreateRequest(
            @NotBlank(message = "이미지 URL은 필수입니다.")
            String imageUrl,
            String description
    ) {}

    public record Response(
            Long id,
            String imageUrl,
            String description,
            LocalDateTime createdAt
    ) {
        public static Response from(Ootd ootd) {
            return new Response(
                    ootd.getId(),
                    ootd.getImageUrl(),
                    ootd.getDescription(),
                    ootd.getCreatedAt()
            );
        }
    }
}
