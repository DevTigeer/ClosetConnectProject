package com.tigger.User.Dto;

public record UserSummary(
        Long id, String email, String nickname, String role
) {
}
