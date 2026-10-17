package com.itibari.auth.service;

import com.itibari.auth.model.response.CountryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CountryService {

    Page<CountryResponse> getCountries(String searchParam, Pageable pageable);

    CountryResponse getCountry(UUID publicId);
}
