package com.tigger.User.Dto;

public record TokenResponse(
        String accessToken,
        UserSummary user
) {
}
