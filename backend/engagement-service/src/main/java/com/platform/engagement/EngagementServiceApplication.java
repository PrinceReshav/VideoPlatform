package com.platform.engagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.platform.engagement.entity", "com.platform.common.entity"})
@EnableJpaRepositories(basePackages = {"com.platform.engagement.repository", "com.platform.common.repository"})
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class EngagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngagementServiceApplication.class, args);
    }
}