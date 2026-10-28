package com.itibari.auth;

import com.itibari.auth.config.CountryInitializer;
import com.itibari.auth.model.Country;
import com.itibari.auth.repository.CountryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CountryInitializerTests extends AuthIssuerApplicationTests {

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private CountryInitializer countryInitializer;

    @Test
    void fillsEmptyAlphaTwoAndCurrencyWithoutRenamingOrInserting() {
        assertEquals(0, countryRepository.count());

        Country country = new Country();
        country.setName("Tanzania");
        country.setIsoAlphaThree("TZA");
        country.setPhoneCode("+255");
        country.setIsoAlphaTwo(null);
        country.setCurrencyCode(null);
        country = countryRepository.saveAndFlush(country);

        UUID publicId = country.getPublicId();
        assertNotNull(publicId);
        long countAfterInsert = countryRepository.count();

        countryInitializer.run(new DefaultApplicationArguments(new String[0]));

        Country filled = countryRepository.findById(country.getId()).orElseThrow();
        assertEquals("Tanzania", filled.getName());
        assertEquals("+255", filled.getPhoneCode());
        assertEquals("TZA", filled.getIsoAlphaThree());
        assertEquals("TZ", filled.getIsoAlphaTwo());
        assertEquals("TZS", filled.getCurrencyCode());
        assertEquals(publicId, filled.getPublicId());
        assertEquals(countAfterInsert, countryRepository.count());

        countryInitializer.run(new DefaultApplicationArguments(new String[0]));

        Country again = countryRepository.findById(country.getId()).orElseThrow();
        assertEquals("Tanzania", again.getName());
        assertEquals("+255", again.getPhoneCode());
        assertEquals("TZ", again.getIsoAlphaTwo());
        assertEquals("TZS", again.getCurrencyCode());
        assertEquals(publicId, again.getPublicId());
        assertEquals(countAfterInsert, countryRepository.count());

        Country antarctica = new Country();
        antarctica.setName("Antarctica");
        antarctica.setIsoAlphaThree("ATA");
        antarctica.setPhoneCode("+672");
        antarctica = countryRepository.saveAndFlush(antarctica);
        long countWithAntarctica = countryRepository.count();

        countryInitializer.run(new DefaultApplicationArguments(new String[0]));

        Country unpriced = countryRepository.findById(antarctica.getId()).orElseThrow();
        assertEquals("Antarctica", unpriced.getName());
        assertEquals("+672", unpriced.getPhoneCode());
        assertEquals("AQ", unpriced.getIsoAlphaTwo());
        assertNull(unpriced.getCurrencyCode());
        Country tanzania = countryRepository.findById(country.getId()).orElseThrow();
        assertEquals("Tanzania", tanzania.getName());
        assertEquals("+255", tanzania.getPhoneCode());
        assertEquals("TZ", tanzania.getIsoAlphaTwo());
        assertEquals("TZS", tanzania.getCurrencyCode());
        assertEquals(countWithAntarctica, countryRepository.count());
    }
}
