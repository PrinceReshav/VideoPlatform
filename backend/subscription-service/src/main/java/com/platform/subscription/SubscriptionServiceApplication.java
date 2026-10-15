package com.platform.subscription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import com.platform.common.entity.Video;
import com.platform.common.repository.VideoRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.platform.subscription.entity", "com.platform.user.entity"}, basePackageClasses = Video.class)
@EnableJpaRepositories(basePackages = {"com.platform.subscription.repository", "com.platform.user.repository"}, basePackageClasses = VideoRepository.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class SubscriptionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscriptionServiceApplication.class, args);
    }
}