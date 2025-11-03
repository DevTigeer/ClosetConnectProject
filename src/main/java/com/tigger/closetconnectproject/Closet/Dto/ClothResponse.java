package com.tigger.closetconnectproject.Closet.Dto;

import com.tigger.closetconnectproject.Closet.Entity.Category;

public record ClothResponse(
        Long id,
        String name,
        Category category,
        String color,
        String brand,
        String imageUrl
) {}
