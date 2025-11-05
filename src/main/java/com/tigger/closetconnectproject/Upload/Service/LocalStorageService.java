package com.tigger.closetconnectproject.Upload.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg","image/png","image/webp");

    @Value("${app.upload.root}")
    private String uploadRoot;

    @Value("${app.upload.public-prefix:/uploads}")
    private String publicPrefix;

    public Stored store(MultipartFile file, Long userId) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("이미지가 비어있습니다.");
        String ct = Optional.ofNullable(file.getContentType()).orElse("");
        if (!ALLOWED.contains(ct)) throw new IllegalArgumentException("jpg/png/webp만 허용됩니다.");

        String ext = switch (ct) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };

        LocalDate today = LocalDate.now();
        Path dir = Paths.get(uploadRoot, String.valueOf(userId),
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()));
        Files.createDirectories(dir);

        String filename = UUID.randomUUID() + ext;
        Path path = dir.resolve(filename);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }

        String imageKey = String.join("/", String.valueOf(userId),
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()),
                filename);

        String publicUrl = publicPrefix + "/" + imageKey;
        return new Stored(imageKey, publicUrl);
    }

    public record Stored(String imageKey, String imageUrl) {}
}
