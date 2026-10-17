package com.itibari.auth.web;

import com.itibari.auth.model.response.CountryResponse;
import com.itibari.auth.service.CountryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Internal lookup for pos-customer / pos-inventory Feign clients.
 */
@RestController
@RequestMapping("/internal/countries")
@Tag(name = "internal-countries")
@RequiredArgsConstructor
public class InternalCountryRestController {

    private final CountryService countryService;

    @GetMapping("/{publicId}")
    @Operation(summary = "Look up a country by public id")
    public ResponseEntity<CountryResponse> getCountry(@PathVariable UUID publicId) {
        return ResponseEntity.ok(countryService.getCountry(publicId));
    }
}
