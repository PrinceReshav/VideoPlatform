package com.platform.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration class to enable AspectJ auto-proxying (Feature 38: Security & Audit Logs)
 */
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {
    // This class enables @Aspect support for audit logging
}