package com.tigger.closetconnectproject.Market;

import com.tigger.closetconnectproject.Closet.Entity.Cloth;
import com.tigger.closetconnectproject.Closet.Entity.Category;
import com.tigger.closetconnectproject.Closet.Repository.ClothRepository;
import com.tigger.closetconnectproject.Market.Dto.MarketProductDtos;
import com.tigger.closetconnectproject.Market.Entity.*;
import com.tigger.closetconnectproject.Market.Repository.MarketProductImageRepository;
import com.tigger.closetconnectproject.Market.Repository.MarketProductLikeRepository;
import com.tigger.closetconnectproject.Market.Repository.MarketProductRepository;
import com.tigger.closetconnectproject.Market.Service.ChatService;
import com.tigger.closetconnectproject.Market.Service.MarketProductService;
import com.tigger.closetconnectproject.User.Entity.UserRole;
import com.tigger.closetconnectproject.User.Entity.UserStatus;
import com.tigger.closetconnectproject.User.Entity.Users;
import com.tigger.closetconnectproject.User.Repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * MarketProductService 단위 테스트
 * - 상품 등록 테스트
 * - 상품 목록 조회 테스트
 * - 상품 상세 조회 테스트
 * - 상품 수정 테스트
 * - 상품 삭제 테스트
 * - 상품 상태 변경 테스트
 * - 조회수 증가 테스트
 */
@ExtendWith(MockitoExtension.class)
class MarketProductServiceTest {

    @Mock MarketProductRepository productRepo;
    @Mock MarketProductImageRepository imageRepo;
    @Mock MarketProductLikeRepository likeRepo;
    @Mock ClothRepository clothRepo;
    @Mock UsersRepository userRepo;
    @Mock ChatService chatService;

    @InjectMocks MarketProductService productService;

    private Users seller;
    private Cloth cloth;
    private MarketProduct product;

    @BeforeEach
    void setUp() {
        seller = Users.builder()
                .userId(1L)
                .email("seller@test.com")
                .nickname("판매자")
                .password("encoded")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();

        cloth = Cloth.builder()
                .id(1L)
                .user(seller)
                .name("나이키 운동화")
                .category(Category.SHOES)
                .imageUrl("/uploads/cloth1.jpg")
                .build();

        product = MarketProduct.builder()
                .id(1L)
                .seller(seller)
                .cloth(cloth)
                .title("나이키 운동화 판매")
                .price(50000)
                .description("깨끗한 상태의 나이키 운동화입니다.")
                .status(ProductStatus.ON_SALE)
                .productCondition(ProductCondition.EXCELLENT)
                .region("서울")
                .brand("나이키")
                .size("270")
                .gender("남성")
                .viewCount(0)
                .build();
    }

    @Test
    void 상품등록_성공() {
        // Given
        var req = new MarketProductDtos.CreateReq();
        req.setClothId(1L);
        req.setTitle("나이키 운동화 판매");
        req.setPrice(50000);
        req.setDescription("깨끗한 상태의 나이키 운동화입니다.");
        req.setProductCondition(ProductCondition.EXCELLENT);
        req.setRegion("서울");
        req.setBrand("나이키");
        req.setSize("270");
        req.setGender("남성");

        given(userRepo.findById(1L)).willReturn(Optional.of(seller));
        given(clothRepo.findById(1L)).willReturn(Optional.of(cloth));
        given(productRepo.save(any(MarketProduct.class))).willReturn(product);
        given(productRepo.findByIdWithDetails(1L)).willReturn(Optional.of(product));
        given(likeRepo.countByMarketProduct_Id(1L)).willReturn(0L);
        given(likeRepo.existsByMarketProduct_IdAndUser_UserId(1L, 1L)).willReturn(false);
        given(imageRepo.findByMarketProduct_IdOrderByOrderIndexAsc(1L)).willReturn(List.of());

        // When
        var result = productService.create(1L, req);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("나이키 운동화 판매");
        assertThat(result.getPrice()).isEqualTo(50000);
        assertThat(result.getStatus()).isEqualTo(ProductStatus.ON_SALE);

        verify(productRepo).save(any(MarketProduct.class));
    }

    @Test
    void 상품등록_실패_옷장아이템없음() {
        // Given
        var req = new MarketProductDtos.CreateReq();
        req.setClothId(999L);
        req.setTitle("나이키 운동화 판매");
        req.setPrice(50000);
        req.setDescription("깨끗한 상태의 나이키 운동화입니다.");
        req.setProductCondition(ProductCondition.EXCELLENT);

        given(userRepo.findById(1L)).willReturn(Optional.of(seller));
        given(clothRepo.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.create(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("옷장 아이템을 찾을 수 없습니다.");
    }

    @Test
    void 상품등록_실패_본인옷장아이템아님() {
        // Given
        var otherUser = Users.builder()
                .userId(2L)
                .email("other@test.com")
                .nickname("다른사람")
                .password("encoded")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();

        var otherCloth = Cloth.builder()
                .id(2L)
                .user(otherUser)
                .name("다른 사람의 옷")
                .category(Category.TOP)
                .build();

        var req = new MarketProductDtos.CreateReq();
        req.setClothId(2L);
        req.setTitle("나이키 운동화 판매");
        req.setPrice(50000);
        req.setDescription("깨끗한 상태의 나이키 운동화입니다.");
        req.setProductCondition(ProductCondition.EXCELLENT);

        given(userRepo.findById(1L)).willReturn(Optional.of(seller));
        given(clothRepo.findById(2L)).willReturn(Optional.of(otherCloth));

        // When & Then
        assertThatThrownBy(() -> productService.create(1L, req))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("본인의 옷장 아이템만 판매할 수 있습니다.");
    }

    @Test
    void 상품목록조회_성공() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<MarketProduct> page = new PageImpl<>(List.of(product), pageable, 1);

        given(productRepo.searchProducts(any(), any(), any(), any())).willReturn(page);
        given(likeRepo.countByMarketProduct_Id(1L)).willReturn(5L);
        given(imageRepo.findByMarketProduct_IdOrderByOrderIndexAsc(1L)).willReturn(List.of());

        // When
        var result = productService.list(null, null, null, 0, 20, "LATEST", null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("나이키 운동화 판매");
        assertThat(result.getContent().get(0).getLikeCount()).isEqualTo(5L);

        verify(productRepo).searchProducts(any(), any(), any(), any());
    }

    @Test
    void 상품상세조회_성공() {
        // Given
        given(productRepo.findByIdWithDetails(1L)).willReturn(Optional.of(product));
        given(likeRepo.countByMarketProduct_Id(1L)).willReturn(5L);
        given(likeRepo.existsByMarketProduct_IdAndUser_UserId(1L, 1L)).willReturn(false);
        given(imageRepo.findByMarketProduct_IdOrderByOrderIndexAsc(1L)).willReturn(List.of());

        // When
        var result = productService.getProductDetail(1L, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("나이키 운동화 판매");
        assertThat(result.getLikeCount()).isEqualTo(5L);
        assertThat(result.isLiked()).isFalse();

        verify(productRepo).findByIdWithDetails(1L);
    }

    @Test
    void 조회수증가_성공() {
        // Given
        given(productRepo.findById(1L)).willReturn(Optional.of(product));

        // When
        productService.incrementViewCount(1L);

        // Then
        assertThat(product.getViewCount()).isEqualTo(1);

        verify(productRepo).findById(1L);
    }

    @Test
    void 상품수정_성공() {
        // Given
        var req = new MarketProductDtos.UpdateReq();
        req.setTitle("나이키 운동화 (가격인하)");
        req.setPrice(40000);

        given(productRepo.findById(1L)).willReturn(Optional.of(product));
        given(productRepo.findByIdWithDetails(1L)).willReturn(Optional.of(product));
        given(likeRepo.countByMarketProduct_Id(1L)).willReturn(0L);
        given(likeRepo.existsByMarketProduct_IdAndUser_UserId(1L, 1L)).willReturn(false);
        given(imageRepo.findByMarketProduct_IdOrderByOrderIndexAsc(1L)).willReturn(List.of());

        // When
        var result = productService.update(1L, 1L, req);

        // Then
        assertThat(result).isNotNull();
        assertThat(product.getTitle()).isEqualTo("나이키 운동화 (가격인하)");
        assertThat(product.getPrice()).isEqualTo(40000);

        verify(productRepo).findById(1L);
    }

    @Test
    void 상품수정_실패_본인아님() {
        // Given
        var req = new MarketProductDtos.UpdateReq();
        req.setTitle("나이키 운동화 (가격인하)");
        req.setPrice(40000);

        given(productRepo.findById(1L)).willReturn(Optional.of(product));

        // When & Then
        assertThatThrownBy(() -> productService.update(1L, 999L, req))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("본인의 상품만 수정할 수 있습니다.");
    }

    @Test
    void 상품상태변경_성공() {
        // Given
        given(productRepo.findById(1L)).willReturn(Optional.of(product));
        given(productRepo.findByIdWithDetails(1L)).willReturn(Optional.of(product));
        given(likeRepo.countByMarketProduct_Id(1L)).willReturn(0L);
        given(likeRepo.existsByMarketProduct_IdAndUser_UserId(1L, 1L)).willReturn(false);
        given(imageRepo.findByMarketProduct_IdOrderByOrderIndexAsc(1L)).willReturn(List.of());
        doNothing().when(chatService).sendSystemMessage(anyLong(), anyString());

        // When
        var result = productService.changeStatus(1L, 1L, ProductStatus.SOLD);

        // Then
        assertThat(result).isNotNull();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD);

        verify(productRepo).findById(1L);
        verify(chatService).sendSystemMessage(eq(1L), anyString());
    }

    @Test
    void 상품삭제_성공() {
        // Given
        given(productRepo.findById(1L)).willReturn(Optional.of(product));
        doNothing().when(imageRepo).deleteByMarketProduct_Id(1L);
        doNothing().when(productRepo).delete(product);

        // When
        productService.delete(1L, 1L);

        // Then
        verify(imageRepo).deleteByMarketProduct_Id(1L);
        verify(productRepo).delete(product);
    }

    @Test
    void 상품삭제_실패_본인아님() {
        // Given
        given(productRepo.findById(1L)).willReturn(Optional.of(product));

        // When & Then
        assertThatThrownBy(() -> productService.delete(1L, 999L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("본인의 상품만 삭제할 수 있습니다.");
    }

    @Test
    void 판매자별상품목록조회_성공() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<MarketProduct> page = new PageImpl<>(List.of(product), pageable, 1);

        given(productRepo.findBySeller_UserId(eq(1L), any())).willReturn(page);
        given(likeRepo.countByMarketProduct_Id(1L)).willReturn(3L);
        given(imageRepo.findByMarketProduct_IdOrderByOrderIndexAsc(1L)).willReturn(List.of());

        // When
        var result = productService.listBySeller(1L, 0, 20, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLikeCount()).isEqualTo(3L);

        verify(productRepo).findBySeller_UserId(eq(1L), any());
    }
}
