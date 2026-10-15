package com.platform.engagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.platform.common.entity.Video;
import com.platform.common.repository.VideoRepository;

@SpringBootApplication
@EntityScan(
        basePackages = "com.platform.engagement.entity",
        basePackageClasses = Video.class
)
@EnableJpaRepositories(
        basePackages = "com.platform.engagement.repository",
        basePackageClasses = VideoRepository.class
)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class EngagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngagementServiceApplication.class, args);
    }
}