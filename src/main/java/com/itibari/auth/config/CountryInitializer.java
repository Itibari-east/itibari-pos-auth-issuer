package com.itibari.auth.config;

import com.itibari.auth.model.Country;
import com.itibari.auth.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class CountryInitializer implements ApplicationRunner {

    private final CountryRepository countryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        ClassPathResource resource = new ClassPathResource("country.csv");
        if (!resource.exists()) {
            log.warn("country.csv not found on classpath; skipping country seed");
            return;
        }
        int upserted = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (header) {
                    header = false;
                    continue;
                }
                String[] parts = line.split(";", -1);
                if (parts.length < 3) {
                    throw new IllegalStateException("Invalid country.csv row: " + line);
                }
                String name = parts[0].trim();
                String iso = parts[1].trim().toUpperCase();
                String phone = parts[2].trim();
                if (name.isBlank() || iso.isBlank() || phone.isBlank()) {
                    throw new IllegalStateException("Country CSV rows require name, ISO alpha-3, and phone code");
                }
                if (!phone.startsWith("+")) {
                    phone = "+" + phone;
                }
                Country country = countryRepository.findByIsoAlphaThreeIgnoreCaseAndDeletedIsFalse(iso)
                        .orElseGet(Country::new);
                country.setName(name);
                country.setIsoAlphaThree(iso);
                country.setPhoneCode(phone);
                countryRepository.save(country);
                upserted++;
            }
        }
        log.info("Seeded/updated {} countries from country.csv", upserted);
    }
}