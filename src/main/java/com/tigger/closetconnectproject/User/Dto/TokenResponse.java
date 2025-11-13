package com.tigger.closetconnectproject.User.Dto;

public record TokenResponse(
        String accessToken,
        UserSummary user
) {
}
