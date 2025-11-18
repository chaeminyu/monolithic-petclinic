package org.springframework.samples.petclinic.gateway.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CustomerServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.customer.url}")
    private String customerServiceUrl;

    @Value("${services.monolith.url}")
    private String monolithUrl;

    public CustomerServiceClient(@Qualifier("customerRestTemplate") RestTemplate customerServiceClient) {
        this.restTemplate = customerServiceClient;
    }

    // Owner 페이지 조회 (Circuit Breaker 적용)
    @CircuitBreaker(name = "customer-service", fallbackMethod = "getOwnerPageFallback")
    public String getOwnerPage(String path) {
        return restTemplate.getForObject(customerServiceUrl + path, String.class);
    }

    // Fallback: 모놀리식에서 조회
    public String getOwnerPageFallback(String path, Exception e) {
        return restTemplate.getForObject(monolithUrl + path, String.class);
    }

    // Pet 페이지 조회
    @CircuitBreaker(name = "customer-service", fallbackMethod = "getPetPageFallback")
    public String getPetPage(String path) {
        return restTemplate.getForObject(customerServiceUrl + path, String.class);
    }

    public String getPetPageFallback(String path, Exception e) {
        return restTemplate.getForObject(monolithUrl + path, String.class);
    }
}