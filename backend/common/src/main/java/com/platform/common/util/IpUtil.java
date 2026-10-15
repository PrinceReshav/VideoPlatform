package com.platform.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for extracting client IP addresses from HTTP requests
 * Handles proxies, load balancers, and various headers (Feature 38: Security & Audit Logs)
 */
public class IpUtil {

    private IpUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Get the client IP address from the request, checking various headers
     * in order of preference to handle proxies and load balancers.
     *
     * @param request the HTTP servlet request
     * @return the client IP address, or null if not available
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String ip = null;

        // Check for various proxy and load balancer headers
        ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs, get the first one
            int commaIndex = ip.indexOf(',');
            if (commaIndex != -1) {
                ip = ip.substring(0, commaIndex);
            }
            return ip.trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        // Fall back to the standard remote address
        return request.getRemoteAddr();
    }
}