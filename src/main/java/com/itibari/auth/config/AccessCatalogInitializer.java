package com.itibari.auth.config;

import com.itibari.auth.service.AccessCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(3)
@RequiredArgsConstructor
public class AccessCatalogInitializer implements ApplicationRunner {

    private final AccessCatalogService accessCatalogService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        accessCatalogService.seedGlobalCatalog();
    }
}
