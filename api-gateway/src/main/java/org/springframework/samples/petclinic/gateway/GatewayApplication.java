package org.springframework.samples.petclinic.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway - 마이크로서비스들의 단일 진입점
 *
 * 기능:
 * - 서비스별 요청 라우팅 (Customer Service vs Monolith)
 * - Feature Toggle (MSA ↔ Monolith 전환)
 * - Circuit Breaker (장애 시 Fallback)
 * - 통합 모니터링
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}