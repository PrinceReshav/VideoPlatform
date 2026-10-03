package com.platform.video;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = {"com.platform.common.entity", "com.platform.user.entity"})
@EnableJpaRepositories(basePackages = {"com.platform.video.repository", "com.platform.user.repository"})
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableScheduling
public class VideoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoServiceApplication.class, args);
    }
}