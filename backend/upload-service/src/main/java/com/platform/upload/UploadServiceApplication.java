package com.platform.upload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.platform.upload.entity", "com.platform.common.entity", "com.platform.user.entity"})
@EnableJpaRepositories(basePackages = {"com.platform.upload.repository", "com.platform.common.repository", "com.platform.user.repository"})
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class UploadServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UploadServiceApplication.class, args);
    }
}