package com.tu.backend.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@ConditionalOnProperty(prefix = "storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FileStorageConfiguration {

    @Bean(destroyMethod = "close")
    public S3Client fileStorageS3Client(FileStorageProperties properties) {
        S3Configuration serviceConfiguration = S3Configuration.builder()
            .pathStyleAccessEnabled(properties.isS3PathStyle())
            .build();

        return S3Client.builder()
            .endpointOverride(URI.create(properties.getS3Endpoint()))
            .region(Region.of(properties.getS3Region()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getS3AccessKey(), properties.getS3SecretKey())
            ))
            .serviceConfiguration(serviceConfiguration)
            .build();
    }
}
