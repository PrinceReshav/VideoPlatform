package com.platform.common.config;

import jakarta.servlet.ServletException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

@Configuration
public class CorrelationIdConfig {

    @Bean
    public OncePerRequestFilter correlationIdFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
                // Generate or extract correlation ID
                String correlationId = request.getHeader("X-Request-ID");
                if (correlationId == null || correlationId.isEmpty()) {
                    correlationId = UUID.randomUUID().toString();
                }

                // Add to MDC for logging
                org.slf4j.MDC.put("X-Request-ID", correlationId);

                // Add to response header
                response.setHeader("X-Request-ID", correlationId);

                try {
                    filterChain.doFilter(request, response);
                } catch (ServletException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    // Clean up MDC
                    org.slf4j.MDC.remove("X-Request-ID");
                }
            }
        };
    }
}