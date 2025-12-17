package com.rds.app_restaurante.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RateLimitConfig implements HandlerInterceptor {

    private final Cache<String, Integer> loginAttempts;
    private final Cache<String, Integer> apiRequests;
    
    @Value("${rate-limit.login.requests-per-minute:5}")
    private int loginRequestsPerMinute;
    
    @Value("${rate-limit.api.requests-per-minute:100}")
    private int apiRequestsPerMinute;

    public RateLimitConfig() {
        // Cache para intentos de login (expira después de 1 minuto)
        this.loginAttempts = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
        
        // Cache para peticiones API (expira después de 1 minuto)
        this.apiRequests = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIpAddress(request);
        String requestPath = request.getRequestURI();

        // Aplicar rate limiting más estricto para login
        if (requestPath.contains("/api/auth/login")) {
            Integer attempts = loginAttempts.getIfPresent(clientIp);
            if (attempts != null && attempts >= loginRequestsPerMinute) {
                log.warn("Rate limit exceeded for login attempt from IP: {}", clientIp);
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                try {
                    response.getWriter().write("{\"message\":\"Demasiados intentos. Por favor, intenta nuevamente en un minuto.\"}");
                } catch (Exception e) {
                    log.error("Error writing rate limit response", e);
                }
                return false;
            }
            loginAttempts.put(clientIp, (attempts == null ? 0 : attempts) + 1);
        } else if (requestPath.startsWith("/api/")) {
            // Rate limiting más flexible para otras API
            Integer requests = apiRequests.getIfPresent(clientIp);
            if (requests != null && requests >= apiRequestsPerMinute) {
                log.warn("Rate limit exceeded for API requests from IP: {}", clientIp);
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                try {
                    response.getWriter().write("{\"message\":\"Demasiadas peticiones. Por favor, espera un momento.\"}");
                } catch (Exception e) {
                    log.error("Error writing rate limit response", e);
                }
                return false;
            }
            apiRequests.put(clientIp, (requests == null ? 0 : requests) + 1);
        }

        return true;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

