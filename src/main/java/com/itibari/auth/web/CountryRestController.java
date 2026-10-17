package com.itibari.auth.web;

import com.itibari.auth.model.response.CountryResponse;
import com.itibari.auth.model.response.PageResponse;
import com.itibari.auth.service.CountryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/countries")
@Tag(name = "countries")
@RequiredArgsConstructor
public class CountryRestController {

    private final CountryService countryService;

    @GetMapping
    @Operation(summary = "List Countries")
    public PageResponse<CountryResponse> getCountries(
            @RequestParam(value = "searchParam", required = false) String searchParam,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(countryService.getCountries(searchParam, pageable));
    }
}
