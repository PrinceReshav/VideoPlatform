package com.platform.moderationvideo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import com.platform.common.entity.Video;
import com.platform.common.repository.VideoRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackageClasses = Video.class)
@EnableJpaRepositories(basePackageClasses = VideoRepository.class)
public class VideoModerationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VideoModerationServiceApplication.class, args);
    }
}
