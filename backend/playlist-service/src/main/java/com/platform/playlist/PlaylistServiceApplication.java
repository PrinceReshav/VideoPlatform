package com.platform.playlist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.platform.playlist.entity", "com.platform.common.entity"})
@EnableJpaRepositories(basePackages = {"com.platform.playlist.repository", "com.platform.common.repository"})
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class PlaylistServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlaylistServiceApplication.class, args);
    }
}
