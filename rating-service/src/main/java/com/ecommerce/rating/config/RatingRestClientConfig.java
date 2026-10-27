package com.ecommerce.rating.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RatingRestClientConfig {
    @Bean
    RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
