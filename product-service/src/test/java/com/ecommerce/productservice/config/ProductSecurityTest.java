package com.ecommerce.productservice.config;

import com.ecommerce.commonlib.security.SecurityAutoConfiguration;
import com.ecommerce.productservice.controller.CategoryController;
import com.ecommerce.productservice.controller.ProductController;
import com.ecommerce.productservice.dto.CategoryDto;
import com.ecommerce.productservice.dto.ProductDto;
import com.ecommerce.productservice.service.CategoryService;
import com.ecommerce.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ProductController.class, CategoryController.class})
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class})
class ProductSecurityTest {

    private static final String EMPTY_JSON = "{}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private ProductService productService;

    @MockBean
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        when(productService.findAll()).thenReturn(List.of());
        when(productService.findById(1)).thenReturn(new ProductDto());
        when(productService.save(org.mockito.ArgumentMatchers.any(ProductDto.class))).thenReturn(new ProductDto());
        when(productService.update(org.mockito.ArgumentMatchers.any(ProductDto.class))).thenReturn(new ProductDto());
        when(productService.update(anyInt(), org.mockito.ArgumentMatchers.any(ProductDto.class)))
                .thenReturn(new ProductDto());
        when(categoryService.findAll()).thenReturn(List.of());
        when(categoryService.findAllCategory(anyInt(), anyInt())).thenReturn(Page.empty());
        when(categoryService.getAllCategories(anyInt(), anyInt(), anyString())).thenReturn(List.of());
        when(categoryService.findById(1)).thenReturn(new CategoryDto());
        when(categoryService.save(org.mockito.ArgumentMatchers.any(CategoryDto.class))).thenReturn(new CategoryDto());
        when(categoryService.update(org.mockito.ArgumentMatchers.any(CategoryDto.class))).thenReturn(new CategoryDto());
        when(categoryService.update(anyInt(), org.mockito.ArgumentMatchers.any(CategoryDto.class)))
                .thenReturn(new CategoryDto());
    }

    @Test
    void unauthenticatedProductCreationIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotCreateUpdateOrDeleteProducts() throws Exception {
        mockMvc.perform(post("/api/products").with(keycloakRoles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPTY_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/products").with(keycloakRoles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPTY_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/products/1").with(keycloakRoles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPTY_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/products/1").with(keycloakRoles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/products/1").with(keycloakRoles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotModifyCategories() throws Exception {
        mockMvc.perform(post("/api/categories").with(keycloakRoles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPTY_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/categories").with(keycloakRoles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPTY_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/categories/1").with(keycloakRoles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPTY_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/categories/1").with(keycloakRoles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanPerformProductAndCategoryOperations() throws Exception {
        mockMvc.perform(post("/api/products").with(keycloakRoles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPTY_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/products").with(keycloakRoles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPTY_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/products/1").with(keycloakRoles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPTY_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/products/1").with(keycloakRoles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/categories").with(keycloakRoles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPTY_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/categories").with(keycloakRoles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPTY_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/categories/1").with(keycloakRoles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(EMPTY_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/categories/1").with(keycloakRoles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void storefrontProductAndCategoryReadsRemainPublic() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
        mockMvc.perform(get("/api/products/1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/api/categories/paging")).andExpect(status().isOk());
        mockMvc.perform(get("/api/categories/paging-and-sorting")).andExpect(status().isOk());
        mockMvc.perform(get("/api/categories/1")).andExpect(status().isOk());
    }

    @Test
    void actuatorEndpointsAreNotPublic() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/env").with(keycloakRoles("USER")))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor keycloakRoles(String... roles) {
        return jwt()
                .jwt(token -> token.claim("realm_access", Map.of("roles", List.of(roles))))
                .authorities(token -> jwtAuthenticationConverter.convert(token).getAuthorities());
    }
}
