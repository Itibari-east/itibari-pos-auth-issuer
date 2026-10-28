package com.itibari.auth.service.impl;

import com.itibari.auth.exception.BadRequestException;
import com.itibari.auth.exception.ConflictException;
import com.itibari.auth.exception.ResourceNotFoundException;
import com.itibari.auth.model.Country;
import com.itibari.auth.model.response.CountryResponse;
import com.itibari.auth.repository.CountryRepository;
import com.itibari.auth.service.CountryCodes;
import com.itibari.auth.service.CountryCodes.CatalogCountry;
import com.itibari.auth.service.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CountryResponse> getCountries(String searchParam, Pageable pageable) {
        if (searchParam == null || searchParam.isBlank()) {
            return countryRepository.list(null, pageable).map(this::overlayCodes);
        }
        Map<String, List<Country>> liveByAlpha3 = liveByAlpha3();
        List<CountryResponse> hits = new ArrayList<>();
        for (CatalogCountry catalog : CountryCodes.search(searchParam)) {
            List<Country> live = liveByAlpha3.getOrDefault(catalog.isoAlphaThree(), List.of());
            if (live.isEmpty()) {
                hits.add(fromCatalog(catalog));
            } else {
                for (Country country : live) {
                    hits.add(toResponse(country));
                }
            }
        }
        hits.sort(Comparator.comparing(CountryResponse::name, String.CASE_INSENSITIVE_ORDER));
        int from = (int) pageable.getOffset();
        if (from >= hits.size()) {
            return new PageImpl<>(List.of(), pageable, hits.size());
        }
        int to = Math.min(from + pageable.getPageSize(), hits.size());
        return new PageImpl<>(hits.subList(from, to), pageable, hits.size());
    }

    @Override
    @Transactional(readOnly = true)
    public CountryResponse getCountry(UUID publicId) {
        Country country = countryRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Country"));
        return toResponse(country);
    }

    @Override
    @Transactional
    public EnsuredCountry ensureCountry(String isoAlpha) {
        if (isoAlpha == null || isoAlpha.isBlank() || !isoAlpha.trim().matches("[A-Za-z]{3}")) {
            throw new BadRequestException("isoAlpha must be a three-letter country code");
        }
        String alphaThree = isoAlpha.trim().toUpperCase(Locale.ROOT);
        CatalogCountry catalog = CountryCodes.byAlpha3(alphaThree);
        if (catalog == null) {
            throw new BadRequestException("Unknown country " + alphaThree);
        }
        List<Country> live = countryRepository.findAllByIsoAlphaThreeIgnoreCaseAndDeletedIsFalse(alphaThree);
        if (live.size() > 1) {
            throw new ConflictException("More than one country exists for " + alphaThree);
        }
        if (live.size() == 1) {
            return new EnsuredCountry(toResponse(live.getFirst()), false);
        }
        if (catalog.dial() == null) {
            throw new BadRequestException("Unknown country " + alphaThree);
        }
        Country country = new Country();
        country.setName(catalog.name());
        country.setIsoAlphaThree(catalog.isoAlphaThree());
        country.setIsoAlphaTwo(catalog.isoAlphaTwo());
        country.setCurrencyCode(catalog.currencyCode());
        country.setPhoneCode(catalog.dial());
        return new EnsuredCountry(toResponse(countryRepository.save(country)), true);
    }

    private Map<String, List<Country>> liveByAlpha3() {
        Map<String, List<Country>> liveByAlpha3 = new HashMap<>();
        for (Country country : countryRepository.findAll()) {
            if (country.isDeleted() || country.getIsoAlphaThree() == null) {
                continue;
            }
            liveByAlpha3
                    .computeIfAbsent(country.getIsoAlphaThree().toUpperCase(Locale.ROOT), key -> new ArrayList<>())
                    .add(country);
        }
        return liveByAlpha3;
    }

    private CountryResponse overlayCodes(CountryResponse country) {
        String alphaTwo = firstPresent(country.isoAlphaTwo(), CountryCodes.alphaTwo(country.isoAlpha()));
        String currency = firstPresent(country.currencyCode(), CountryCodes.currency(alphaTwo));
        return new CountryResponse(
                country.publicId(),
                country.name(),
                country.isoAlpha(),
                country.countryCode(),
                alphaTwo,
                currency);
    }

    private CountryResponse toResponse(Country country) {
        String alphaTwo = firstPresent(country.getIsoAlphaTwo(), CountryCodes.alphaTwo(country.getIsoAlphaThree()));
        String currency = firstPresent(country.getCurrencyCode(), CountryCodes.currency(alphaTwo));
        return new CountryResponse(
                country.getPublicId(),
                country.getName(),
                country.getIsoAlphaThree(),
                country.getPhoneCode(),
                alphaTwo,
                currency);
    }

    private static CountryResponse fromCatalog(CatalogCountry catalog) {
        return new CountryResponse(
                null,
                catalog.name(),
                catalog.isoAlphaThree(),
                catalog.dial(),
                catalog.isoAlphaTwo(),
                catalog.currencyCode());
    }

    private static String firstPresent(String stored, String fallback) {
        if (stored == null || stored.isBlank()) {
            return fallback;
        }
        return stored;
    }
}
