package com.itibari.auth;

import com.itibari.auth.config.FileClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
@EnableConfigurationProperties(FileClientProperties.class)
public class AuthIssuerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthIssuerApplication.class, args);
    }
}
