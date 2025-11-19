package org.springframework.samples.petclinic.gateway.controller;

import org.springframework.beans.factory.annotation.Qualifier;
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

    public GatewayController(CustomerServiceClient customerClient, @Qualifier("monolithRestTemplate") RestTemplate monolithClient) {
        this.customerClient = customerClient;
        this.monolithClient = monolithClient;
    }

    // Owner 관련 요청 라우팅
    @RequestMapping("/owners/**")
    public String routeOwnerRequests(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (useCustomerService) {
            // Customer Service로 리다이렉트
            return "redirect:http://localhost:8081" + path;
        } else {
            // Monolith로 리다이렉트
            return "redirect:" + monolithUrl + path;
        }
    }

    // Pet 관련 요청 라우팅
    @RequestMapping("/pets/**")
    public String routePetRequests(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (useCustomerService) {
            return "redirect:http://localhost:8081" + path;
        } else {
            return "redirect:" + monolithUrl + path;
        }
    }

    // Vet 관련 요청 (항상 Monolith)
    @RequestMapping("/vets/**")
    public String routeVetRequests(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "redirect:" + monolithUrl + path;
    }

    // Visit 관련 요청 (항상 Monolith)
    @RequestMapping("/visits/**")
    public String routeVisitRequests(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "redirect:" + monolithUrl + path;
    }

    // 메인 페이지
    @GetMapping("/")
    public String home() {
        return "redirect:" + monolithUrl + "/";
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