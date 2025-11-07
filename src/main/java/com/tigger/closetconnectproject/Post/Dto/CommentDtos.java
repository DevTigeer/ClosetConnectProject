package com.tigger.closetconnectproject.Post.Dto;

import com.tigger.closetconnectproject.Post.Entity.Comment;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

public class CommentDtos {

    @Getter @Setter @NoArgsConstructor
    public static class CreateReq {
        @NotBlank private String content;
        private Long parentId; // 대댓글(optional)
    }

    @Getter @Setter @NoArgsConstructor
    public static class UpdateReq {
        private String content;
    }

    @Getter @Builder
    public static class CommentRes {
        private Long id;
        private Long postId;
        private Long parentId;
        private String content;
        private String authorName;
        private LocalDateTime createdAt;

        public static CommentRes of(Comment c) {
            return CommentRes.builder()
                    .id(c.getId())
                    .postId(c.getPost().getId())
                    .parentId(c.getParent() != null ? c.getParent().getId() : null)
                    .content(c.getContent())
                    .authorName(c.getAuthor() != null ? c.getAuthor().getNickname() : "unknown")
                    .createdAt(c.getCreatedAt())
                    .build();
        }
    }
}
