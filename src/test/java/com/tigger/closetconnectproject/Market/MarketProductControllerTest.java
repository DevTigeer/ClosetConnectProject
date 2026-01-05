package com.tigger.closetconnectproject.Market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigger.closetconnectproject.Market.Controller.MarketProductController;
import com.tigger.closetconnectproject.Market.Dto.MarketProductDtos;
import com.tigger.closetconnectproject.Market.Entity.ProductCondition;
import com.tigger.closetconnectproject.Market.Entity.ProductStatus;
import com.tigger.closetconnectproject.Market.Service.MarketProductLikeService;
import com.tigger.closetconnectproject.Market.Service.MarketProductService;
import com.tigger.closetconnectproject.Security.AppUserDetails;
import com.tigger.closetconnectproject.User.Entity.UserRole;
import com.tigger.closetconnectproject.User.Entity.UserStatus;
import com.tigger.closetconnectproject.User.Entity.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MarketProductController 단위 테스트
 * - 상품 목록 조회 테스트
 * - 상품 상세 조회 테스트
 * - 상품 등록 테스트
 * - 상품 수정 테스트
 * - 상품 삭제 테스트
 * - 상품 상태 변경 테스트
 */
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = MarketProductController.class)
class MarketProductControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockBean AuditorAware<Long> auditorAware;
    @MockBean MarketProductService productService;
    @MockBean MarketProductLikeService likeService;

    private MarketProductDtos.ProductListRes PRODUCT1;
    private MarketProductDtos.ProductListRes PRODUCT2;
    private MarketProductDtos.ProductDetailRes PRODUCT_DETAIL;
    private Page<MarketProductDtos.ProductListRes> PAGE;

    @BeforeEach
    void setUp() {
        PRODUCT1 = MarketProductDtos.ProductListRes.builder()
                .productId(1L)
                .title("나이키 운동화")
                .price(50000)
                .status(ProductStatus.ON_SALE)
                .imageUrl("/uploads/image1.jpg")
                .region("서울")
                .likeCount(5L)
                .viewCount(100)
                .createdAt(LocalDateTime.now())
                .build();

        PRODUCT2 = MarketProductDtos.ProductListRes.builder()
                .productId(2L)
                .title("아디다스 티셔츠")
                .price(30000)
                .status(ProductStatus.ON_SALE)
                .imageUrl("/uploads/image2.jpg")
                .region("부산")
                .likeCount(3L)
                .viewCount(50)
                .createdAt(LocalDateTime.now())
                .build();

        PRODUCT_DETAIL = MarketProductDtos.ProductDetailRes.builder()
                .productId(1L)
                .title("나이키 운동화")
                .price(50000)
                .description("깨끗한 상태의 나이키 운동화입니다.")
                .status(ProductStatus.ON_SALE)
                .productCondition(ProductCondition.EXCELLENT)
                .region("서울")
                .brand("나이키")
                .size("270")
                .gender("남성")
                .viewCount(100)
                .likeCount(5L)
                .liked(false)
                .isMine(false)
                .sellerId(1L)
                .sellerNickname("판매자")
                .imageUrl("/uploads/image1.jpg")
                .clothId(1L)
                .clothName("운동화")
                .clothCategory("SHOES")
                .images(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PAGE = new PageImpl<>(List.of(PRODUCT1, PRODUCT2), PageRequest.of(0, 20), 2);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(productService, likeService);
        Mockito.reset(productService, likeService);
    }

    @Test
    void 상품목록조회_성공_200() throws Exception {
        // Given
        given(productService.list(any(), any(), any(), anyInt(), anyInt(), anyString(), any()))
                .willReturn(PAGE);

        // When & Then
        mvc.perform(get("/api/v1/market/products")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "LATEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title").value("나이키 운동화"))
                .andExpect(jsonPath("$.content[1].title").value("아디다스 티셔츠"));

        verify(productService).list(isNull(), isNull(), isNull(), eq(0), eq(20), eq("LATEST"), isNull());
    }

    @Test
    void 상품목록조회_필터링_성공_200() throws Exception {
        // Given
        given(productService.list(any(), any(), any(), anyInt(), anyInt(), anyString(), any()))
                .willReturn(PAGE);

        // When & Then
        mvc.perform(get("/api/v1/market/products")
                        .param("status", "ON_SALE")
                        .param("region", "서울")
                        .param("keyword", "나이키")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        verify(productService).list(eq(ProductStatus.ON_SALE), eq("서울"), eq("나이키"), eq(0), eq(20), eq("LATEST"), isNull());
    }

    @Test
    void 상품상세조회_성공_200() throws Exception {
        // Given
        given(productService.getProductDetail(eq(1L), any()))
                .willReturn(PRODUCT_DETAIL);
        doNothing().when(productService).incrementViewCount(eq(1L));

        // When & Then
        mvc.perform(get("/api/v1/market/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1L))
                .andExpect(jsonPath("$.title").value("나이키 운동화"))
                .andExpect(jsonPath("$.price").value(50000))
                .andExpect(jsonPath("$.status").value("ON_SALE"));

        verify(productService).incrementViewCount(eq(1L));
        verify(productService).getProductDetail(eq(1L), isNull());
    }

    @Test
    @WithMockUser
    void 상품등록_성공_201() throws Exception {
        // Given
        var mockUser = Users.builder()
                .userId(1L)
                .email("seller@test.com")
                .nickname("판매자")
                .password("encoded")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();
        var principal = new AppUserDetails(mockUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        var req = new MarketProductDtos.CreateReq();
        req.setClothId(1L);
        req.setTitle("나이키 운동화");
        req.setPrice(50000);
        req.setDescription("깨끗한 상태의 나이키 운동화입니다.");
        req.setProductCondition(ProductCondition.EXCELLENT);
        req.setRegion("서울");
        req.setBrand("나이키");
        req.setSize("270");
        req.setGender("남성");

        given(productService.create(eq(1L), any(MarketProductDtos.CreateReq.class)))
                .willReturn(PRODUCT_DETAIL);

        // When & Then
        mvc.perform(post("/api/v1/market/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1L))
                .andExpect(jsonPath("$.title").value("나이키 운동화"));

        verify(productService).create(eq(1L), any(MarketProductDtos.CreateReq.class));
    }

    @Test
    @WithMockUser
    void 상품수정_성공_200() throws Exception {
        // Given
        var mockUser = Users.builder()
                .userId(1L)
                .email("seller@test.com")
                .nickname("판매자")
                .password("encoded")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();
        var principal = new AppUserDetails(mockUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        var req = new MarketProductDtos.UpdateReq();
        req.setTitle("나이키 운동화 (가격인하)");
        req.setPrice(40000);

        given(productService.update(eq(1L), eq(1L), any(MarketProductDtos.UpdateReq.class)))
                .willReturn(PRODUCT_DETAIL);

        // When & Then
        mvc.perform(patch("/api/v1/market/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1L));

        verify(productService).update(eq(1L), eq(1L), any(MarketProductDtos.UpdateReq.class));
    }

    @Test
    @WithMockUser
    void 상품상태변경_성공_200() throws Exception {
        // Given
        var mockUser = Users.builder()
                .userId(1L)
                .email("seller@test.com")
                .nickname("판매자")
                .password("encoded")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();
        var principal = new AppUserDetails(mockUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        var req = new MarketProductDtos.ChangeStatusReq();
        req.setStatus(ProductStatus.SOLD);

        given(productService.changeStatus(eq(1L), eq(1L), eq(ProductStatus.SOLD)))
                .willReturn(PRODUCT_DETAIL);

        // When & Then
        mvc.perform(patch("/api/v1/market/products/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1L));

        verify(productService).changeStatus(eq(1L), eq(1L), eq(ProductStatus.SOLD));
    }

    @Test
    @WithMockUser
    void 상품삭제_성공_204() throws Exception {
        // Given
        var mockUser = Users.builder()
                .userId(1L)
                .email("seller@test.com")
                .nickname("판매자")
                .password("encoded")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.NORMAL)
                .build();
        var principal = new AppUserDetails(mockUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        doNothing().when(productService).delete(eq(1L), eq(1L));

        // When & Then
        mvc.perform(delete("/api/v1/market/products/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(productService).delete(eq(1L), eq(1L));
    }

    @Test
    void 판매자별상품목록조회_성공_200() throws Exception {
        // Given
        given(productService.listBySeller(eq(1L), anyInt(), anyInt(), any()))
                .willReturn(PAGE);

        // When & Then
        mvc.perform(get("/api/v1/market/products/seller/{sellerId}", 1L)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        verify(productService).listBySeller(eq(1L), eq(0), eq(20), isNull());
    }
}
