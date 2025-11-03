package com.tigger.closetconnectproject.Closet.Dto;

import com.tigger.closetconnectproject.Closet.Entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClothCreateRequest(
        @NotBlank String name,
        @NotNull  Category category,
        String color,
        String brand,
        String imageUrl
) {}
