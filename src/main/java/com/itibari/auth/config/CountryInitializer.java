package com.itibari.auth.config;

import com.itibari.auth.model.Country;
import com.itibari.auth.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.Locale;
import java.util.MissingResourceException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CountryInitializer implements ApplicationRunner {

    private final CountryRepository countryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int filled = 0;
        for (Country country : countryRepository.findAll()) {
            if (country.isDeleted()) {
                continue;
            }
            if (fillEmptyCodes(country)) {
                countryRepository.save(country);
                filled++;
            }
        }
        if (filled > 0) {
            log.info("Filled empty alpha-2 or currency on {} countries", filled);
        }
    }

    private static boolean fillEmptyCodes(Country country) {
        boolean changed = false;
        if (blank(country.getIsoAlphaTwo())) {
            String alphaTwo = alphaTwo(country.getIsoAlphaThree());
            if (alphaTwo != null) {
                country.setIsoAlphaTwo(alphaTwo);
                changed = true;
            } else if (!blank(country.getIsoAlphaThree())) {
                log.warn("No alpha-2 for country {} ({})", country.getPublicId(), country.getIsoAlphaThree());
            }
        }
        if (blank(country.getCurrencyCode())) {
            String currency = currency(country.getIsoAlphaTwo());
            if (currency != null) {
                country.setCurrencyCode(currency);
                changed = true;
            } else if (!blank(country.getIsoAlphaTwo())) {
                log.warn("No currency for country {} ({})", country.getPublicId(), country.getIsoAlphaTwo());
            }
        }
        return changed;
    }

    private static String alphaTwo(String isoAlphaThree) {
        if (blank(isoAlphaThree)) {
            return null;
        }
        String alphaThree = isoAlphaThree.trim().toUpperCase(Locale.ROOT);
        for (String country : Locale.getISOCountries()) {
            try {
                if (alphaThree.equals(Locale.of("", country).getISO3Country())) {
                    return country;
                }
            } catch (MissingResourceException ex) {
                // This ISO country has no alpha-3 in the JDK.
            }
        }
        return null;
    }

    private static String currency(String isoAlphaTwo) {
        if (blank(isoAlphaTwo)) {
            return null;
        }
        try {
            Currency currency = Currency.getInstance(Locale.of("", isoAlphaTwo.trim().toUpperCase(Locale.ROOT)));
            if (currency == null) {
                return null;
            }
            return currency.getCurrencyCode();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
