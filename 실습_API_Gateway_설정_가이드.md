# API Gateway 설정 가이드
## 📋 2단계: Customer Service와 Monolith 통합

---

## 🎯 실습 목표

**전제 조건**: Customer Service 분리 완료 (1단계)
- ✅ Customer Service가 8081 포트에서 실행 중
- ✅ 기존 Monolith가 8080 포트에서 실행 중

**목표**: API Gateway로 두 서비스를 통합하여 단일 진입점 제공

### 아키텍처
```
Before (분리된 상태):
[Client] → [Customer Service:8081] (Owner/Pet)
[Client] → [Monolith:8080]        (Vet/Visit)

After (Gateway 통합):
[Client] → [API Gateway:8090] → [Customer Service:8081] (Owner/Pet)
                              → [Monolith:8080]        (Vet/Visit)
```

---

## 🚀 Phase 2: API Gateway 구현

### Step 1: 프로젝트 생성

```bash
# API Gateway 프로젝트 생성
mkdir api-gateway
mkdir -p api-gateway/src/main/java/org/springframework/samples/petclinic/gateway/{controller,config,client}
mkdir -p api-gateway/src/main/resources
```

### Step 2: build.gradle 설정

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '2.7.18'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'org.springframework.samples'
version = '1.0.0'
sourceCompatibility = '17'

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Web
    implementation 'org.springframework.boot:spring-boot-starter-web'
    
    // Circuit Breaker (장애 대응)
    implementation 'io.github.resilience4j:resilience4j-spring-boot2:1.7.1'
    
    // Actuator (모니터링)
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### Step 3: application.yml 설정

```yaml
server:
  port: 8090

spring:
  application:
    name: api-gateway

# 서비스 URL 설정
services:
  customer:
    url: http://localhost:8081
  monolith:
    url: http://localhost:8080

# Feature Toggle (MSA ↔ Monolith 전환)
feature:
  toggle:
    use-customer-service: true  # true = Customer Service, false = Monolith

# Circuit Breaker 설정 (장애 대응)
resilience4j:
  circuitbreaker:
    instances:
      customer-service:
        register-health-indicator: true
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10000
        permitted-number-of-calls-in-half-open-state: 3

# 로깅
logging:
  level:
    org.springframework.samples.petclinic.gateway: DEBUG
    io.github.resilience4j: DEBUG

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,circuitbreakers
  endpoint:
    health:
      show-details: always
```

### Step 4: RestClient 설정

```java
// RestClientConfig.java
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
    
    @Bean
    public RestTemplate customerServiceClient() {
        return new RestTemplate();
    }
    
    @Bean
    public RestTemplate monolithClient() {
        return new RestTemplate();
    }
}
```

### Step 5: Service Client 구현

```java
// CustomerServiceClient.java
package org.springframework.samples.petclinic.gateway.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
    
    public CustomerServiceClient(RestTemplate customerServiceClient) {
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
```

### Step 6: Gateway Controller 구현

```java
// GatewayController.java
package org.springframework.samples.petclinic.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.samples.petclinic.gateway.client.CustomerServiceClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class GatewayController {
    
    private final CustomerServiceClient customerClient;
    private final RestTemplate monolithClient;
    
    @Value("${feature.toggle.use-customer-service}")
    private boolean useCustomerService;
    
    @Value("${services.monolith.url}")
    private String monolithUrl;
    
    public GatewayController(CustomerServiceClient customerClient, RestTemplate monolithClient) {
        this.customerClient = customerClient;
        this.monolithClient = monolithClient;
    }
    
    // Owner 관련 요청 라우팅
    @RequestMapping("/owners/**")
    public String routeOwnerRequests(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        if (useCustomerService) {
            // Customer Service로 라우팅
            return customerClient.getOwnerPage(path);
        } else {
            // Monolith로 라우팅
            return monolithClient.getForObject(monolithUrl + path, String.class);
        }
    }
    
    // Pet 관련 요청 라우팅  
    @RequestMapping("/pets/**")
    public String routePetRequests(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        if (useCustomerService) {
            return customerClient.getPetPage(path);
        } else {
            return monolithClient.getForObject(monolithUrl + path, String.class);
        }
    }
    
    // Vet 관련 요청 (항상 Monolith)
    @RequestMapping("/vets/**")
    public String routeVetRequests(HttpServletRequest request) {
        String path = request.getRequestURI();
        return monolithClient.getForObject(monolithUrl + path, String.class);
    }
    
    // Visit 관련 요청 (항상 Monolith)
    @RequestMapping("/visits/**")
    public String routeVisitRequests(HttpServletRequest request) {
        String path = request.getRequestURI();
        return monolithClient.getForObject(monolithUrl + path, String.class);
    }
    
    // 메인 페이지
    @GetMapping("/")
    public String home() {
        return monolithClient.getForObject(monolithUrl + "/", String.class);
    }
    
    // Health Check
    @GetMapping("/health")
    @ResponseBody
    public Map<String, Object> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("gateway", "UP");
        status.put("customerService", useCustomerService ? "ACTIVE" : "INACTIVE");
        status.put("monolith", "ACTIVE");
        return status;
    }
    
    // 아키텍처 정보
    @GetMapping("/architecture")
    @ResponseBody
    public Map<String, String> getArchitecture() {
        Map<String, String> architecture = new HashMap<>();
        architecture.put("type", "Hybrid MSA");
        architecture.put("customerService", useCustomerService ? "Microservice" : "Monolith");
        architecture.put("vetService", "Monolith");
        architecture.put("visitService", "Monolith");
        return architecture;
    }
}
```

### Step 7: Main Application 클래스

```java
// GatewayApplication.java
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
```

---

## 🧪 테스트 및 실행

### Step 1: 서비스 실행

```bash
# 모든 명령은 루트 디렉토리(monolithic-petclinic)에서 실행

# Terminal 1: Monolith (기존)
./gradlew monolith:bootRun
# → http://localhost:8080

# Terminal 2: Customer Service (1단계에서 생성)
./gradlew customer-service:bootRun
# → http://localhost:8081

# Terminal 3: API Gateway (새로 생성)
./gradlew api-gateway:bootRun
# → http://localhost:8090
```

### Step 2: 기능 테스트

#### 1. Health Check
```bash
curl http://localhost:8090/health

# 예상 응답:
{
  "gateway": "UP",
  "customerService": "ACTIVE", 
  "monolith": "ACTIVE"
}
```

#### 2. 아키텍처 확인
```bash
curl http://localhost:8090/architecture

# 예상 응답:
{
  "type": "Hybrid MSA",
  "customerService": "Microservice",
  "vetService": "Monolith",
  "visitService": "Monolith"
}
```

#### 3. 라우팅 테스트
- `http://localhost:8090/owners` → Customer Service (8081)로 라우팅
- `http://localhost:8090/vets` → Monolith (8080)로 라우팅
- `http://localhost:8090/` → Monolith 메인 페이지

### Step 3: Feature Toggle 테스트

```yaml
# application.yml에서 설정 변경
feature:
  toggle:
    use-customer-service: false  # Customer Service 비활성화
```

재시작 후:
- `http://localhost:8090/owners` → Monolith (8080)로 라우팅

### Step 4: Circuit Breaker 테스트

1. **Customer Service 중지** (Terminal 2에서 Ctrl+C)
2. **Owner 페이지 접속**: `http://localhost:8090/owners`
3. **결과**: 자동으로 Monolith로 Fallback
4. **Customer Service 재시작** 후 다시 Customer Service로 라우팅

---

## ✅ 완료 체크리스트

- [ ] API Gateway가 8090 포트에서 실행됨
- [ ] Owner/Pet 요청이 Customer Service로 라우팅됨
- [ ] Vet/Visit 요청이 Monolith로 라우팅됨
- [ ] Feature Toggle로 Customer Service ↔ Monolith 전환 가능
- [ ] Circuit Breaker로 장애 시 자동 Fallback
- [ ] Health Check API 동작
- [ ] 클라이언트는 8090만 사용하면 모든 기능 접근 가능

---

## 🎯 핵심 성과

1. **단일 진입점**: 클라이언트는 Gateway(8090)만 알면 됨
2. **투명한 라우팅**: 사용자는 서비스 분리를 모름
3. **장애 대응**: Customer Service 장애 시 자동 Fallback
4. **점진적 전환**: Feature Toggle로 안전한 배포
5. **모니터링**: 통합된 Health Check 및 아키텍처 정보

아기사자들의 API Gateway를 통한 하이브리드 MSA 아키텍처 구축을 축하합니다~~