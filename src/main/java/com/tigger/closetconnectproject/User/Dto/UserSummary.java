package com.tigger.closetconnectproject.User.Dto;

public record UserSummary(
        Long id, String email, String nickname, String role
) {
}
