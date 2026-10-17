package com.itibari.auth.service.impl;

import com.itibari.auth.exception.ResourceNotFoundException;
import com.itibari.auth.model.Country;
import com.itibari.auth.model.response.CountryResponse;
import com.itibari.auth.repository.CountryRepository;
import com.itibari.auth.service.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CountryResponse> getCountries(String searchParam, Pageable pageable) {
        return countryRepository.list(searchParam, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public CountryResponse getCountry(UUID publicId) {
        Country country = countryRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Country"));
        return toResponse(country);
    }

    private static CountryResponse toResponse(Country country) {
        return new CountryResponse(country.getPublicId(), country.getName(), country.getIsoAlphaThree(),
                country.getPhoneCode());
    }
}
