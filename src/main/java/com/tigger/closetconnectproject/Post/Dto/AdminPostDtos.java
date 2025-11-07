package com.tigger.closetconnectproject.Post.Dto;

import com.tigger.closetconnectproject.Post.Entity.PostStatus;
import lombok.*;

public class AdminPostDtos {

    @Getter @Setter @NoArgsConstructor
    public static class UpdateStatusReq {
        private PostStatus status;   // NORMAL, HIDDEN, BLINDED, DELETED
        private String reason;       // 선택(로그 남길 때 사용)
    }

    @Getter @Setter @NoArgsConstructor
    public static class PinReq {
        private boolean pinned;      // true=핀 고정, false=해제
    }

    @Getter @Setter @NoArgsConstructor
    public static class MoveReq {
        private Long toBoardId;      // 게시글을 옮길 보드ID
    }
}
