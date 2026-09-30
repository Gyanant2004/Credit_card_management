package com.ofss.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient cardServiceClient(
            @Value("${card.service.url}") String cardServiceUrl) {

        return RestClient.builder()
                .baseUrl(cardServiceUrl)
                .build();
    }
}