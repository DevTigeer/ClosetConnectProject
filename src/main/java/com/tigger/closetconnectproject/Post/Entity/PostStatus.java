package com.tigger.closetconnectproject.Post.Entity;

/** 게시글 상태 */
public enum PostStatus {
    NORMAL,   // 정상 노출
    HIDDEN,   // 숨김(작성자 또는 관리자 처리)
    BLINDED,  // 블라인드(관리자 사유로 차단)
    DELETED   // 소프트 삭제
}
