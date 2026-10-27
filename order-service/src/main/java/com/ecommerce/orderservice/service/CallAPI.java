package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.product.ProductDto;
import com.ecommerce.orderservice.dto.user.UserDto;
import com.ecommerce.commonlib.viewmodel.ApiResponse;
import com.ecommerce.commonlib.exception.BusinessException;
import com.ecommerce.commonlib.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

@Component
@RequiredArgsConstructor
public class CallAPI {

    private final RestClient.Builder restClientBuilder;

    public UserDto receiverCurrentUserDto(String authorizationHeader) {
        try {
            ApiResponse<UserDto> response = restClientBuilder.baseUrl("http://auth-service:8088").build()
                    .get()
                    .uri("/auth/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<ApiResponse<UserDto>>() {});
        if (response == null || response.data() == null || response.data().getId() == null) {
            throw new IllegalStateException("Authenticated user identity was not returned by auth-service");
        }
        return response.data();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 401) throw BusinessException.of(ErrorCode.UNAUTHORIZED);
            if (ex.getStatusCode().value() == 403) throw BusinessException.of(ErrorCode.FORBIDDEN);
            if (ex.getStatusCode().value() == 404) throw BusinessException.notFound("auth.user.not.found");
            throw BusinessException.of(ErrorCode.SERVICE_UNAVAILABLE);
        } catch (ResourceAccessException ex) {
            throw BusinessException.of(ErrorCode.SERVICE_UNAVAILABLE);
        } catch (RuntimeException ex) {
            throw BusinessException.of(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    public ProductDto receiverProductDto(Integer productId) {
        return restClientBuilder.baseUrl("http://product-service:8086").build()
                .get()
                .uri("/product/api/products/{id}", productId)
                .retrieve()
                .body(ProductDto.class);
    }
}
