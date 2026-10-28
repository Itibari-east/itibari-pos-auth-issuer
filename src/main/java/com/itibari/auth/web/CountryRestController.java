package com.itibari.auth.web;

import com.itibari.auth.doc.Examples;
import com.itibari.auth.model.request.CreateCountryRequest;
import com.itibari.auth.model.response.CountryResponse;
import com.itibari.auth.model.response.CurrencyResponse;
import com.itibari.auth.model.response.PageResponse;
import com.itibari.auth.service.CountryService;
import com.itibari.auth.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/countries")
@Tag(name = "countries")
@RequiredArgsConstructor
public class CountryRestController {

    private final CountryService countryService;
    private final CurrencyService currencyService;

    @GetMapping
    @Operation(summary = "List Countries", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "Tanzania live row", value = Examples.COUNTRY_TANZANIA_HIT),
                            @ExampleObject(name = "Germany catalog hit", value = Examples.COUNTRY_GERMANY_HIT)
                    }))
    })
    public PageResponse<CountryResponse> getCountries(
            @RequestParam(value = "searchParam", required = false) String searchParam,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(countryService.getCountries(searchParam, pageable));
    }

    @PostMapping
    @Operation(summary = "Ensure one country",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
                    examples = @ExampleObject(name = "Kenya alpha-3", value = Examples.CREATE_COUNTRY_REQUEST))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Existing row", content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = Examples.COUNTRY_TANZANIA_HIT))),
                    @ApiResponse(responseCode = "201", description = "Created", content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = Examples.COUNTRY_GERMANY_CREATED)))
            })
    public ResponseEntity<CountryResponse> createCountry(@Valid @RequestBody CreateCountryRequest request) {
        CountryService.EnsuredCountry result = countryService.ensureCountry(request.isoAlpha());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.country());
    }

    @GetMapping("/currencies")
    @Operation(summary = "List currencies", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "Tanzanian Shilling", value = Examples.CURRENCY_TZS_HIT)))
    })
    public PageResponse<CurrencyResponse> getCurrencies(
            @RequestParam(value = "searchParam", required = false) String searchParam,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(currencyService.getCurrencies(searchParam, pageable));
    }
}
