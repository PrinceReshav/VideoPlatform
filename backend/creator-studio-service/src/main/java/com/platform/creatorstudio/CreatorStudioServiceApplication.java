package com.platform.creatorstudio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CreatorStudioServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CreatorStudioServiceApplication.class, args);
    }
}