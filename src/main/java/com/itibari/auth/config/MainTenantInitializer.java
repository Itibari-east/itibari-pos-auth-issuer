package com.itibari.auth.config;

import com.itibari.auth.model.Institution;
import com.itibari.auth.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MainTenantInitializer implements ApplicationRunner {

    private final InstitutionRepository institutionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (institutionRepository.findByDeletedIsFalseAndMainTenantIsTrue().isPresent()) {
            return;
        }
        Institution main = Institution.builder()
                .name("Itibari")
                .identityNumber("MAIN-TENANT")
                .mainTenant(true)
                .parentTenant(false)
                .active(true)
                .country("TZ")
                .build();
        institutionRepository.save(main);
        log.info("Seeded main tenant institution");
    }
}
