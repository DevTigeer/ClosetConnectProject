package com.tigger.closetconnectproject.Post.Entity;

/** 게시글 공개 범위 */
public enum Visibility {
    PUBLIC,      // 모두 조회 가능
    PRIVATE,     // 작성자/관리자만
    ADMIN_ONLY   // 관리자 전용
}
