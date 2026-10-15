package com.platform.transcoding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import com.platform.common.entity.ProcessingJob;
import com.platform.common.repository.ProcessingJobRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackageClasses = ProcessingJob.class)
@EnableJpaRepositories(basePackageClasses = ProcessingJobRepository.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync
@EnableScheduling
public class TranscodingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TranscodingServiceApplication.class, args);
    }
}