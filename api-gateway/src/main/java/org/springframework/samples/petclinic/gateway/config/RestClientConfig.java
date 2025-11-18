package org.springframework.samples.petclinic.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Value("${services.customer.url}")
    private String customerServiceUrl;

    @Value("${services.monolith.url}")
    private String monolithUrl;

    @Bean("customerRestTemplate")
    public RestTemplate customerServiceClient() {
        return new RestTemplate();
    }

    @Bean("monolithRestTemplate") 
    public RestTemplate monolithClient() {
        return new RestTemplate();
    }
}