package com.tigger.closetconnectproject.Post.Service;

import com.tigger.closetconnectproject.Post.Dto.PostDtos;
import com.tigger.closetconnectproject.Post.Entity.Post;
import com.tigger.closetconnectproject.Post.Entity.PostAttachment;
import com.tigger.closetconnectproject.Post.Repository.PostAttachmentRepository;
import com.tigger.closetconnectproject.Post.Repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final PostRepository postRepository;
    private final PostAttachmentRepository attachmentRepository;

    @Value("${file.upload-dir:uploads}") // 기본 uploads
    private String uploadDir;

    private static String ext(String filename) {
        if (!StringUtils.hasText(filename)) return "";
        int i = filename.lastIndexOf('.');
        return (i > -1 && i < filename.length()-1) ? filename.substring(i+1) : "";
    }

    @Transactional
    public PostDtos.AttachmentRes upload(Long postId, Long userId, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file empty");
        }

        // 게시글 존재/권한 등은 상위(PostService)에서 검사하는 흐름이라면 생략 가능
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("post not found: " + postId));

        LocalDate today = LocalDate.now();
        String orig = file.getOriginalFilename();
        String extension = ext(orig);
        String random = UUID.randomUUID().toString().replace("-", "");
        String savedName = random + (StringUtils.hasText(extension) ? ("." + extension) : "");

        // /uploads/{postId}/YYYY/MM/DD/
        String relPath = String.format("%d/%04d/%02d/%02d", postId, today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        File dir = new File(uploadDir, relPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("failed to create upload dir: " + dir.getAbsolutePath());
        }

        File dest = new File(dir, savedName);
        file.transferTo(dest);

        String contentType = Files.probeContentType(dest.toPath());
        long size = dest.length();
        String url = "/uploads/" + relPath + "/" + savedName; // 정적 리소스 매핑 필요(아래 참고)

        PostAttachment att = PostAttachment.builder()
                .post(post)
                .filename(orig)
//                .storedName(savedName)
                .contentType(contentType)
                .size(size)
                .url(url)
                .build();

        attachmentRepository.save(att);

        return PostDtos.AttachmentRes.builder()
                .id(att.getId())
                .url(att.getUrl())
                .filename(att.getFilename())
                .contentType(att.getContentType())
                .size(att.getSize())
                .build();
    }
}
