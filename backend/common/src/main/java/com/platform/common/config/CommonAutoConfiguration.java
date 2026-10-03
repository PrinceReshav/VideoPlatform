package com.platform.common.config;

import com.platform.common.audit.AuditListener;
import com.platform.common.kafka.KafkaTopicNames;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.common.security.PlatformPermissionEvaluator;
import com.platform.common.storage.MinioStorageProvider;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import com.platform.common.storage.StorageProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@AutoConfiguration
@Import(CommonSecurityConfiguration.class)
public class CommonAutoConfiguration {

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof com.platform.common.security.PlatformUserDetails userDetails) {
                return Optional.of(userDetails.getUserId());
            }
            return Optional.empty();
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public PlatformPermissionEvaluator platformPermissionEvaluator() {
        return new PlatformPermissionEvaluator();
    }

    @Bean
    public CurrentUserAccessor currentUserAccessor() {
        return new CurrentUserAccessor();
    }


    @Bean
    public MinioClient minioClient(
            @Value("${minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${minio.access-key:minioadmin}") String accessKey,
            @Value("${minio.secret-key:minioadmin123}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public StorageProvider storageProvider(MinioClient minioClient) {
        return new MinioStorageProvider(minioClient);
    }

    @Bean
    public AuditListener auditListener() {
        return new AuditListener();
    }
}