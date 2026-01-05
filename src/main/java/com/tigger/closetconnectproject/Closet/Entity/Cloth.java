package com.tigger.closetconnectproject.Closet.Entity;

import com.tigger.closetconnectproject.User.Entity.Users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "cloth")
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Cloth {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 16)  // nullable로 변경 (AI 처리 후 사용자 확인)
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 512)
    private String imageUrl;  // 기존 호환성 유지 (deprecated 예정)

    @Column(name = "original_image_url", length = 512)
    private String originalImageUrl;  // 원본 이미지 URL

    @Column(name = "removed_bg_image_url", length = 512)
    private String removedBgImageUrl;  // 배경 제거된 이미지 URL

    @Column(name = "segmented_image_url", length = 512)
    private String segmentedImageUrl;  // 크롭된 옷 이미지 (세그멘테이션 결과)

    @Column(name = "inpainted_image_url", length = 512)
    private String inpaintedImageUrl;  // 복원된 최종 이미지 (AI inpainting 결과)

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PROCESSING;  // 처리 상태

    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_category", length = 16)
    private Category suggestedCategory;  // AI가 제안한 카테고리

    @Column(name = "segmentation_label", length = 50)
    private String segmentationLabel;  // AI 원본 라벨 (e.g., "upper-clothes", "pants")

    @Column(name = "error_message", length = 1000)
    private String errorMessage;  // 에러 발생 시 메시지

    @Column(name = "current_step", length = 100)
    private String currentStep;  // 현재 진행 중인 단계 (e.g., "배경 제거 중", "세그멘테이션 중")

    @Column(name = "progress_percentage")
    private Integer progressPercentage;  // 진행률 (0-100)

    @Column(nullable = false)
    @Builder.Default
    private Boolean confirmed = false;  // 사용자가 최종 이미지를 선택했는지 여부

    @Column(name = "additional_items_json", columnDefinition = "TEXT")
    private String additionalItemsJson;  // 추가 감지된 아이템들 (JSON 형식, deprecated)

    @Column(name = "all_segmented_items_json", columnDefinition = "TEXT")
    private String allSegmentedItemsJson;  // 모든 크롭된 아이템들 (JSON 형식, 크기순 정렬)

    @Column(name = "all_expanded_items_json", columnDefinition = "TEXT")
    private String allExpandedItemsJson;  // 모든 Gemini 확장된 아이템들 (JSON 형식, 크기순 정렬)

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
