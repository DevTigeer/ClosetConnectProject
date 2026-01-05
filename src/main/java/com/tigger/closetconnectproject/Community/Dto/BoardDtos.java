package com.tigger.closetconnectproject.Community.Dto;

import com.tigger.closetconnectproject.Community.Entity.CommunityBoard;
import jakarta.validation.constraints.*;

public class BoardDtos {

    public record BoardRes(
            Long id, String name, String slug, String type, String visibility,
            boolean system, Integer sortOrder
    ) {
        public static BoardRes of(CommunityBoard b) {
            return new BoardRes(
                    b.getId(), b.getName(), b.getSlug(),
                    b.getType().name(), b.getVisibility().name(),
                    b.isSystem(), b.getSortOrder()
            );
        }
    }

    public record CreateBoardReq(
            @NotBlank String name,
            @Pattern(regexp = "^[a-z0-9\\-]{2,50}$") String slug,
            @NotBlank String type,              // FREE/ITEM/PROMO/OOTD/SELECT/MARKET
            @NotBlank String visibility,        // PUBLIC/PRIVATE/HIDDEN/ARCHIVED
            boolean system,
            Integer sortOrder
    ) {}

    public record UpdateBoardReq(
            @Size(min=1, max=50) String name,
            Integer sortOrder
    ) {}

    public record ChangeVisibilityReq(
            @NotBlank String visibility
    ) {}
}

