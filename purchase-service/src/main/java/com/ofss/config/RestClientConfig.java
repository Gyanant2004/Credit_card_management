package com.ofss.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /*
     * Normal RestClient.Builder.
     *
     * This is the default builder in the application and is NOT
     * load-balanced. This prevents Eureka's own communication
     * with http://localhost:8761 from being sent through the
     * service-name load balancer.
     */
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /*
     * Load-balanced RestClient.Builder.
     *
     * We use this one only when Purchase Service communicates
     * with another microservice using its Eureka service name.
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}