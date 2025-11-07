package com.tigger.closetconnectproject.Post.Dto;

import com.tigger.closetconnectproject.Post.Entity.Post;
import com.tigger.closetconnectproject.Post.Entity.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class PostDtos {

    @Getter @Setter @NoArgsConstructor
    public static class CreateReq {
        @NotBlank private String title;
        @NotBlank private String content;
        @NotNull  private Visibility visibility = Visibility.PUBLIC;
    }

    @Getter @Setter @NoArgsConstructor
    public static class UpdateReq {
        private String title;
        private String content;
        private Visibility visibility;
    }

    @Getter @Builder
    public static class AttachmentRes {
        private Long id;
        private String url;
        private String filename;
        private String contentType;
        private long size;
    }

    @Getter @Builder
    public static class PostRes {
        private Long id;
        private Long boardId;
        private String title;
        private String content;
        private String authorName;
        private boolean likedByMe;
        private long viewCount;
        private long likeCount;
        private List<AttachmentRes> attachments;
        private LocalDateTime createdAt;

        public static PostRes of(Post p, boolean liked, List<AttachmentRes> atts) {
            return PostRes.builder()
                    .id(p.getId())
                    .boardId(p.getBoard().getId())
                    .title(p.getTitle())
                    .content(p.getContent())
                    .authorName(p.getAuthor() != null ? p.getAuthor().getNickname() : "unknown")
                    .likedByMe(liked)
                    .viewCount(p.getViewCount())
                    .likeCount(p.getLikeCount())
                    .attachments(atts)
                    .createdAt(p.getCreatedAt())
                    .build();
        }
    }
}
