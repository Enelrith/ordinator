package com.enelrith.ordinator;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
    }

    @Bean
    LocalStackContainer localStackContainer() {
        return new LocalStackContainer(
                DockerImageName.parse("localstack/localstack:latest"))
                .withServices("s3")
                .withEnv("LOCALSTACK_AUTH_TOKEN", System.getenv("LOCALSTACK_AUTH_TOKEN"));
    }

    @Bean
    DynamicPropertyRegistrar s3Properties(LocalStackContainer localstack) {
        return registry -> {
            registry.add("localstack.s3.endpoint",
                    () -> localstack.getEndpoint().toString());
            registry.add("localstack.s3.accessKey", localstack::getAccessKey);
            registry.add("localstack.s3.secretKey", localstack::getSecretKey);
            registry.add("localstack.s3.bucketName",
                    () -> "ordinator-attachments");
        };
    }

}
