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

    @Builder
    @Getter
    public static class PostRes {
        private Long id;
        private String title;
        private String content;
        private String authorName;
        private Long authorId;
        private Long likeCount;
        private Long viewCount;
        private LocalDateTime createdAt;
        private List<AttachmentRes> attachments;
        private boolean liked;

        public static PostRes of(Post p, boolean liked, List<AttachmentRes> atts) {
            String authorName = (p.getAuthor() != null && p.getAuthor().getNickname() != null)
                    ? p.getAuthor().getNickname()
                    : "익명";

            return PostRes.builder()
                    .id(p.getId())
                    .title(p.getTitle())
                    .content(p.getContent())
                    .authorId(p.getAuthor() != null ? p.getAuthor().getUserId() : null)
                    .authorName(authorName)
                    .likeCount(p.getLikeCount())
                    .viewCount(p.getViewCount())
                    .createdAt(p.getCreatedAt())
                    .attachments(atts)
                    .liked(liked)
                    .build();
        }
    }

}
